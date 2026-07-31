use agent::{build_collector_registry, MonitoringAgent};
use anyhow::{anyhow, Result};
use browser::{BrowserController, BrowserPolicy};
use chrono::Utc;
use config::{ClientSettings, CollectorConfig, FeatureFlags, RemoteConfig};
use network::{
    parse_interview_link, ApiClient, AuthRequest, AuthResponse, InterviewContext, SessionStartRequest,
};
use policy::{PolicyEngine, PolicyRule, PolicySet, Severity};
use security::{generate_device_id, AuthSession, ConsentRecord};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use storage::LocalStore;
use system::{device_summary, run_system_checks, SystemCheck};
use tauri::{AppHandle, Emitter};
use telemetry::TelemetryPanelEvent;
use tokio::sync::mpsc;
use uuid::Uuid;

pub struct AppState {
    inner: Arc<Mutex<StateInner>>,
    panel_rx: Mutex<Option<mpsc::UnboundedReceiver<TelemetryPanelEvent>>>,
}

struct StateInner {
    store: LocalStore,
    api: ApiClient,
    device_id: String,
    auth: Option<AuthSession>,
    session_id: Option<Uuid>,
    interview_id: Option<Uuid>,
    link_token: Option<String>,
    interview: Option<InterviewContext>,
    remote_config: RemoteConfig,
    consent_granted: bool,
    browser: BrowserController,
    monitoring_handle: Option<tauri::async_runtime::JoinHandle<()>>,
    panel_tx: Option<mpsc::UnboundedSender<TelemetryPanelEvent>>,
    settings: ClientSettings,
    client_version: String,
}

#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LaunchContext {
    pub interview_id: Uuid,
    pub link_token: String,
    pub device_id: String,
    pub client_version: String,
}

#[derive(Debug, Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AppInfo {
    pub product_name: String,
    pub client_version: String,
    pub device_id: String,
    pub session_id: Option<Uuid>,
    pub consent_granted: bool,
}

impl AppState {
    pub fn new() -> Result<Self> {
        let data_dir = dirs::data_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("IntegrityPro");
        std::fs::create_dir_all(&data_dir)?;
        let db_path = data_dir.join("client.db");
        let store = LocalStore::open(db_path.to_string_lossy().as_ref())?;
        let settings = store.load_settings().unwrap_or_default();
        let device_id = generate_device_id();
        let api = ApiClient::new(
            std::env::var("INTEGRITY_API_URL").unwrap_or_else(|_| "http://localhost:8080".into()),
        );

        let (panel_tx, panel_rx) = mpsc::unbounded_channel();

        Ok(Self {
            inner: Arc::new(Mutex::new(StateInner {
                store,
                api,
                device_id,
                auth: None,
                session_id: None,
                interview_id: None,
                link_token: None,
                interview: None,
                remote_config: RemoteConfig::default(),
                consent_granted: false,
                browser: BrowserController::new(BrowserPolicy::enterprise_default()),
                monitoring_handle: None,
                panel_tx: Some(panel_tx),
                settings,
                client_version: env!("CARGO_PKG_VERSION").into(),
            })),
            panel_rx: Mutex::new(Some(panel_rx)),
        })
    }

    pub fn start_panel_event_bridge(&self, app: AppHandle) {
        let inner = Arc::clone(&self.inner);
        let mut rx = self
            .panel_rx
            .lock()
            .expect("panel receiver lock")
            .take()
            .expect("panel receiver already taken");

        tauri::async_runtime::spawn(async move {
            while let Some(event) = rx.recv().await {
                let _ = app.emit("telemetry-panel-event", &event);
                if let Ok(guard) = inner.lock() {
                    if guard.remote_config.feature_flags.enable_debug_mode {
                        tracing::debug!(message = %event.message, "telemetry panel event");
                    }
                }
            }
        });
    }

    fn with_inner<T>(&self, f: impl FnOnce(&mut StateInner) -> Result<T>) -> Result<T> {
        let mut guard = self.inner.lock().map_err(|_| anyhow!("application state poisoned"))?;
        f(&mut guard)
    }

    pub fn launch_context(&self, args: &[String]) -> Result<LaunchContext> {
        self.with_inner(|state| {
            let link = args
                .iter()
                .find(|arg| arg.contains("interview") || arg.starts_with("integritypro://"))
                .cloned()
                .unwrap_or_else(|| "integritypro://interview/demo?token=demo".into());
            let (interview_id, link_token) = parse_interview_link(&link)?;
            state.interview_id = Some(interview_id);
            state.link_token = Some(link_token.clone());
            Ok(LaunchContext {
                interview_id,
                link_token,
                device_id: state.device_id.clone(),
                client_version: state.client_version.clone(),
            })
        })
    }

    pub async fn authenticate(&self, email: String, password: String) -> Result<AuthResponse> {
        let (api, request, device_id) = self.with_inner(|state| {
            Ok((
                state.api.clone(),
                AuthRequest {
                    email,
                    password,
                    device_id: state.device_id.clone(),
                },
                state.device_id.clone(),
            ))
        })?;

        let response = api.authenticate(&request).await?;

        self.with_inner(|state| {
            state.auth = Some(AuthSession {
                access_token: response.access_token.clone(),
                refresh_token: response.refresh_token.clone(),
                expires_at: response.expires_at,
                device_id: device_id.clone(),
            });
            let encoded = security::obfuscate(&response.access_token, &device_id);
            let _ = state.store.save_auth_token(&encoded);
            Ok(response)
        })
    }

    pub fn system_checks(&self) -> Result<Vec<SystemCheck>> {
        self.with_inner(|state| Ok(run_system_checks(&state.client_version)))
    }

    pub async fn load_remote_config(&self) -> Result<RemoteConfig> {
        let (api, token) = self.with_inner(|state| {
            let token = state
                .auth
                .as_ref()
                .map(|auth| auth.access_token.clone())
                .or_else(|| state.link_token.clone())
                .ok_or_else(|| anyhow!("not authenticated"))?;
            Ok((state.api.clone(), token))
        })?;

        let config = api.fetch_remote_config(&token).await?;
        self.with_inner(|state| {
            state.remote_config = config.clone();
            Ok(config)
        })
    }

    pub async fn load_interview(&self) -> Result<InterviewContext> {
        let (api, token, interview_id) = self.with_inner(|state| {
            let interview_id = state
                .interview_id
                .ok_or_else(|| anyhow!("missing interview id from launch link"))?;
            let token = state
                .auth
                .as_ref()
                .map(|auth| auth.access_token.clone())
                .or_else(|| state.link_token.clone())
                .ok_or_else(|| anyhow!("not authenticated"))?;
            Ok((state.api.clone(), token, interview_id))
        })?;

        let interview = api.fetch_interview(&token, interview_id).await?;
        self.with_inner(|state| {
            state.interview = Some(interview.clone());
            Ok(interview)
        })
    }

    pub fn accept_consent(&self, categories: Vec<String>) -> Result<()> {
        self.with_inner(|state| {
            let session_id = state.session_id.unwrap_or_else(Uuid::new_v4);
            state.session_id = Some(session_id);
            state.consent_granted = true;
            let record = ConsentRecord {
                session_id,
                granted_at: Utc::now(),
                organization_name: state.remote_config.organization_name.clone(),
                categories,
            };
            state.store.save_consent(&record)?;
            Ok(())
        })
    }

    pub fn decline_consent(&self) -> Result<()> {
        self.with_inner(|state| {
            state.consent_granted = false;
            Ok(())
        })
    }

    pub async fn start_interview(&self) -> Result<InterviewContext> {
        let snapshot = self.with_inner(|state| {
            if !state.consent_granted {
                return Err(anyhow!("consent must be granted before monitoring starts"));
            }
            let interview = state
                .interview
                .clone()
                .ok_or_else(|| anyhow!("interview details not loaded"))?;
            let token = state
                .auth
                .as_ref()
                .map(|auth| auth.access_token.clone())
                .or_else(|| state.link_token.clone())
                .ok_or_else(|| anyhow!("missing auth token"))?;
            let interview_id = state.interview_id.unwrap_or(interview.id);
            let device_id = state.device_id.clone();
            let api = state.api.clone();
            let store_path = dirs::data_dir()
                .unwrap_or_else(|| PathBuf::from("."))
                .join("IntegrityPro/client.db");
            Ok((interview, token, interview_id, device_id, api, store_path))
        })?;

        let (interview, token, interview_id, device_id, api, store_path) = snapshot;
        let session = api
            .start_session(
                &token,
                &SessionStartRequest {
                    interview_id,
                    device_id: device_id.clone(),
                    device_summary: device_summary(),
                },
            )
            .await?;

        self.with_inner(|state| {
            state.session_id = Some(session.id);
            state.browser
                .navigate(&interview.meeting_url)
                .map_err(|e| anyhow!(e))?;
            let panel_tx = state
                .panel_tx
                .clone()
                .ok_or_else(|| anyhow!("telemetry panel channel unavailable"))?;
            let store = LocalStore::open(store_path.to_string_lossy().as_ref())?;

            let collector_config = CollectorConfig::new(state.remote_config.feature_flags.clone());
            let collectors = build_collector_registry(&collector_config);
            let policy = PolicyEngine::new(PolicySet {
                rules: vec![
                    PolicyRule {
                        code: "BROWSER_FOCUS_LOST".into(),
                        enabled: true,
                        severity: Severity::Medium,
                    },
                    PolicyRule {
                        code: "VM_DETECTED".into(),
                        enabled: true,
                        severity: Severity::Critical,
                    },
                ],
            });

            let streaming_enabled = state.remote_config.feature_flags.enable_telemetry_streaming;
            let agent = MonitoringAgent::new(
                session.id,
                token,
                collectors,
                policy,
                api,
                store,
                collector_config,
                panel_tx,
                streaming_enabled,
            );

            let handle = tauri::async_runtime::spawn(async move {
                if let Err(error) = agent.run().await {
                    tracing::error!(?error, "monitoring agent stopped");
                }
            });
            state.monitoring_handle = Some(handle);
            Ok(interview)
        })
    }

    pub async fn end_session(&self) -> Result<serde_json::Value> {
        let snapshot = self.with_inner(|state| {
            let session_id = state
                .session_id
                .ok_or_else(|| anyhow!("no active session"))?;
            let token = state
                .auth
                .as_ref()
                .map(|auth| auth.access_token.clone())
                .or_else(|| state.link_token.clone())
                .unwrap_or_default();
            let api = state.api.clone();
            if let Some(handle) = state.monitoring_handle.take() {
                handle.abort();
            }
            Ok((session_id, token, api))
        })?;

        let (session_id, token, api) = snapshot;
        let summary = serde_json::json!({
            "sessionId": session_id,
            "endedAt": Utc::now(),
            "integrityScore": 92,
            "status": "completed"
        });
        api.submit_integrity_summary(&token, session_id, &summary).await?;
        Ok(summary)
    }

    pub fn browser_policy(&self) -> Result<BrowserPolicy> {
        self.with_inner(|state| Ok(state.browser.policy().clone()))
    }

    pub fn validate_navigation(&self, url: String) -> Result<bool> {
        self.with_inner(|state| Ok(state.browser.policy().allows(&url)))
    }

    pub fn settings(&self) -> Result<ClientSettings> {
        self.with_inner(|state| Ok(state.settings.clone()))
    }

    pub fn update_settings(&self, settings: ClientSettings) -> Result<ClientSettings> {
        self.with_inner(|state| {
            state.settings = settings.clone();
            state.store.save_settings(&settings)?;
            Ok(settings)
        })
    }

    pub fn feature_flags(&self) -> Result<FeatureFlags> {
        self.with_inner(|state| Ok(state.remote_config.feature_flags.clone()))
    }

    pub fn app_info(&self) -> Result<AppInfo> {
        self.with_inner(|state| {
            Ok(AppInfo {
                product_name: "Integrity Pro".into(),
                client_version: state.client_version.clone(),
                device_id: state.device_id.clone(),
                session_id: state.session_id,
                consent_granted: state.consent_granted,
            })
        })
    }
}
