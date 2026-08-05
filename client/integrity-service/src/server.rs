use agent::{build_collector_registry, MonitoringAgent};
use anyhow::{anyhow, Result};
use browser::{BrowserController, BrowserPolicy};
use chrono::Utc;
use config::{ClientSettings, CollectorConfig, RemoteConfig};
use ipc::{IpcRequest, IpcResponse, IpcServer};
use network::{
    parse_interview_link, ApiClient, AuthRequest, InterviewContext, SessionStartRequest,
};
use policy::{default_policy_set, PolicyEngine};
use security::{generate_device_id, obfuscate, AuthSession, ConsentRecord};
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use storage::LocalStore;
use system::{device_summary, run_system_checks};
use telemetry::TelemetryPanelEvent;
use tokio::sync::mpsc;
use tokio::sync::mpsc::UnboundedSender;
use uuid::Uuid;

pub struct ServiceState {
    inner: Arc<Mutex<StateInner>>,
    panel_tx: Option<UnboundedSender<TelemetryPanelEvent>>,
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
    monitoring_handle: Option<tokio::task::JoinHandle<()>>,
    shutdown_tx: Option<mpsc::UnboundedSender<()>>,
    settings: ClientSettings,
    client_version: String,
    violation_count: usize,
    event_count: usize,
    last_event: String,
    start_time: Option<chrono::DateTime<Utc>>,
}

impl ServiceState {
    fn new() -> Result<Self> {
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

        let (panel_tx, mut panel_rx) = mpsc::unbounded_channel::<TelemetryPanelEvent>();
        tokio::spawn(async move {
            while let Some(event) = panel_rx.recv().await {
                tracing::debug!(message = %event.message, kind = ?event.kind, "telemetry event");
            }
        });

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
                shutdown_tx: None,
                settings,
                client_version: env!("CARGO_PKG_VERSION").into(),
                violation_count: 0,
                event_count: 0,
                last_event: String::new(),
                start_time: None,
            })),
            panel_tx: Some(panel_tx),
        })
    }

    fn with_inner<T>(&self, f: impl FnOnce(&mut StateInner) -> Result<T>) -> Result<T> {
        let mut guard = self.inner.lock().map_err(|_| anyhow!("service state poisoned"))?;
        f(&mut guard)
    }

    async fn handle_request(
        &self,
        request: IpcRequest,
    ) -> IpcResponse {
        match request {
            IpcRequest::Ping => IpcResponse::Pong,

            IpcRequest::LaunchContext { args } => match self.with_inner(|state| {
                let link = args
                    .iter()
                    .find(|arg| arg.contains("interview") || arg.starts_with("integritypro://"))
                    .cloned()
                    .unwrap_or_else(|| "integritypro://interview/demo?token=demo".into());
                let (interview_id, link_token) = parse_interview_link(&link)?;
                state.interview_id = Some(interview_id);
                state.link_token = Some(link_token.clone());
                Ok(serde_json::json!({
                    "interviewId": interview_id,
                    "linkToken": link_token,
                    "deviceId": state.device_id,
                    "clientVersion": state.client_version,
                }))
            }) {
                Ok(v) => IpcResponse::Ok(v),
                Err(e) => IpcResponse::Error { code: "LAUNCH_CONTEXT_ERROR".into(), message: e.to_string() },
            },

            IpcRequest::Authenticate { email, password } => {
                let (api, request, device_id) = match self.with_inner(|state| {
                    Ok((
                        state.api.clone(),
                        AuthRequest { email, password, device_id: state.device_id.clone() },
                        state.device_id.clone(),
                    ))
                }) {
                    Ok(v) => v,
                    Err(e) => return IpcResponse::Error { code: "AUTH_PREP_ERROR".into(), message: e.to_string() },
                };

                match api.authenticate(&request).await {
                    Ok(response) => {
                        let _ = self.with_inner(|state| {
                            state.auth = Some(AuthSession {
                                access_token: response.access_token.clone(),
                                refresh_token: response.refresh_token.clone(),
                                expires_at: response.expires_at,
                                device_id: device_id.clone(),
                            });
                            let encoded = obfuscate(&response.access_token, &device_id);
                            let _ = state.store.save_auth_token(&encoded);
                            Ok(())
                        });
                        IpcResponse::Ok(serde_json::json!({
                            "accessToken": response.access_token,
                            "refreshToken": response.refresh_token,
                            "expiresAt": response.expires_at,
                        }))
                    }
                    Err(e) => IpcResponse::Error { code: "AUTH_FAILED".into(), message: e.to_string() },
                }
            }

            IpcRequest::GetSystemChecks => match self.with_inner(|state| {
                Ok(serde_json::to_value(run_system_checks(&state.client_version))?)
            }) {
                Ok(v) => IpcResponse::Ok(v),
                Err(e) => IpcResponse::Error { code: "SYSTEM_CHECKS_ERROR".into(), message: e.to_string() },
            },

            IpcRequest::GetRemoteConfig => {
                let (api, token) = match self.with_inner(|state| {
                    let token = state.auth.as_ref().map(|a| a.access_token.clone())
                        .or_else(|| state.link_token.clone())
                        .ok_or_else(|| anyhow!("not authenticated"))?;
                    Ok((state.api.clone(), token))
                }) {
                    Ok(v) => v,
                    Err(e) => return IpcResponse::Error { code: "CONFIG_AUTH_ERROR".into(), message: e.to_string() },
                };

                match api.fetch_remote_config(&token).await {
                    Ok(config) => {
                        let _ = self.with_inner(|state| {
                            state.remote_config = config.clone();
                            Ok(())
                        });
                        IpcResponse::Ok(serde_json::to_value(config).unwrap_or_default())
                    }
                    Err(e) => IpcResponse::Error { code: "CONFIG_FETCH_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::GetInterview => {
                let (api, token, interview_id) = match self.with_inner(|state| {
                    let interview_id = state.interview_id.ok_or_else(|| anyhow!("missing interview id"))?;
                    let token = state.auth.as_ref().map(|a| a.access_token.clone())
                        .or_else(|| state.link_token.clone())
                        .ok_or_else(|| anyhow!("not authenticated"))?;
                    Ok((state.api.clone(), token, interview_id))
                }) {
                    Ok(v) => v,
                    Err(e) => return IpcResponse::Error { code: "INTERVIEW_PREP_ERROR".into(), message: e.to_string() },
                };

                match api.fetch_interview(&token, interview_id).await {
                    Ok(interview) => {
                        let _ = self.with_inner(|state| {
                            state.interview = Some(interview.clone());
                            Ok(())
                        });
                        IpcResponse::Ok(serde_json::to_value(interview).unwrap_or_default())
                    }
                    Err(e) => IpcResponse::Error { code: "INTERVIEW_FETCH_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::AcceptConsent { categories } => {
                match self.with_inner(|state| {
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
                }) {
                    Ok(()) => IpcResponse::Ok(serde_json::json!({"status": "accepted"})),
                    Err(e) => IpcResponse::Error { code: "CONSENT_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::DeclineConsent => {
                let _ = self.with_inner(|state| {
                    state.consent_granted = false;
                    Ok(())
                });
                IpcResponse::Ok(serde_json::json!({"status": "declined"}))
            }

            IpcRequest::StartInterview => {
                let snapshot = match self.with_inner(|state| {
                    if !state.consent_granted {
                        return Err(anyhow!("consent not granted"));
                    }
                    let interview = state.interview.clone().ok_or_else(|| anyhow!("interview not loaded"))?;
                    let token = state.auth.as_ref().map(|a| a.access_token.clone())
                        .or_else(|| state.link_token.clone())
                        .ok_or_else(|| anyhow!("missing auth token"))?;
                    let interview_id = state.interview_id.unwrap_or(interview.id);
                    let device_id = state.device_id.clone();
                    let api = state.api.clone();
                    let store_path = dirs::data_dir()
                        .unwrap_or_else(|| PathBuf::from("."))
                        .join("IntegrityPro/client.db");
                    let panel_tx = self.panel_tx.clone();
                    Ok((interview, token, interview_id, device_id, api, store_path, panel_tx))
                }) {
                    Ok(v) => v,
                    Err(e) => return IpcResponse::Error { code: "INTERVIEW_START_ERROR".into(), message: e.to_string() },
                };

                let (interview, token, interview_id, device_id, api, store_path, panel_tx) = snapshot;

                let session = match api.start_session(&token, &SessionStartRequest {
                    interview_id,
                    device_id: device_id.clone(),
                    device_summary: device_summary(),
                }).await {
                    Ok(s) => s,
                    Err(e) => return IpcResponse::Error { code: "SESSION_START_ERROR".into(), message: e.to_string() },
                };

                match self.with_inner(|state| {
                    state.session_id = Some(session.id);
                    state.start_time = Some(Utc::now());
                    state.violation_count = 0;
                    state.event_count = 0;

                    let panel_tx = panel_tx.ok_or_else(|| anyhow!("panel channel unavailable"))?;
                    let store = LocalStore::open(store_path.to_string_lossy().as_ref())?;
                    let collector_config = CollectorConfig::new(state.remote_config.feature_flags.clone());
                    let collectors = build_collector_registry(&collector_config);
                    let policy = PolicyEngine::new(default_policy_set());
                    let streaming_enabled = state.remote_config.feature_flags.enable_telemetry_streaming;
                    let (shutdown_tx, shutdown_rx) = mpsc::unbounded_channel();
                    state.shutdown_tx = Some(shutdown_tx);

                    let mut agent = MonitoringAgent::new(
                        session.id,
                        token,
                        collectors,
                        policy,
                        api.clone(),
                        store,
                        collector_config,
                        panel_tx,
                        shutdown_rx,
                        streaming_enabled,
                    );

                    let handle = tokio::spawn(async move {
                        if let Err(error) = agent.run().await {
                            tracing::error!(?error, "monitoring agent stopped");
                        }
                    });
                    state.monitoring_handle = Some(handle);
                    Ok(())
                }) {
                    Ok(()) => IpcResponse::Ok(serde_json::to_value(&interview).unwrap_or_default()),
                    Err(e) => IpcResponse::Error { code: "MONITORING_START_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::EndSession => {
                let snapshot = match self.with_inner(|state| {
                    let session_id = state.session_id.ok_or_else(|| anyhow!("no active session"))?;
                    let token = state.auth.as_ref().map(|a| a.access_token.clone())
                        .or_else(|| state.link_token.clone())
                        .unwrap_or_default();
                    let api = state.api.clone();
                    if let Some(tx) = state.shutdown_tx.take() {
                        let _ = tx.send(());
                    }
                    Ok((session_id, token, api))
                }) {
                    Ok(v) => v,
                    Err(e) => return IpcResponse::Error { code: "SESSION_END_ERROR".into(), message: e.to_string() },
                };

                let (session_id, token, api) = snapshot;
                let summary = serde_json::json!({
                    "sessionId": session_id,
                    "endedAt": Utc::now(),
                    "integrityScore": 92,
                    "status": "completed"
                });
                let _ = api.submit_integrity_summary(&token, session_id, &summary).await;
                IpcResponse::Ok(summary)
            }

            IpcRequest::GetSettings => {
                match self.with_inner(|state| Ok(serde_json::to_value(&state.settings)?)) {
                    Ok(v) => IpcResponse::Ok(v),
                    Err(e) => IpcResponse::Error { code: "SETTINGS_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::UpdateSettings { settings } => {
                match self.with_inner(|state| {
                    let s: ClientSettings = serde_json::from_value(settings)?;
                    state.settings = s.clone();
                    state.store.save_settings(&s)?;
                    Ok(serde_json::to_value(&s)?)
                }) {
                    Ok(v) => IpcResponse::Ok(v),
                    Err(e) => IpcResponse::Error { code: "SETTINGS_UPDATE_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::GetFeatureFlags => {
                match self.with_inner(|state| Ok(serde_json::to_value(&state.remote_config.feature_flags)?)) {
                    Ok(v) => IpcResponse::Ok(v),
                    Err(e) => IpcResponse::Error { code: "FEATURE_FLAGS_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::GetAppInfo => {
                match self.with_inner(|state| Ok(serde_json::json!({
                    "productName": "Integrity Pro",
                    "clientVersion": state.client_version,
                    "deviceId": state.device_id,
                    "sessionId": state.session_id,
                    "consentGranted": state.consent_granted,
                }))) {
                    Ok(v) => IpcResponse::Ok(v),
                    Err(e) => IpcResponse::Error { code: "APP_INFO_ERROR".into(), message: e.to_string() },
                }
            }

            IpcRequest::GetStatus => {
                match self.with_inner(|state| Ok(serde_json::json!({
                    "connected": true,
                    "authenticated": state.auth.is_some(),
                    "consentGranted": state.consent_granted,
                    "sessionActive": state.session_id.is_some(),
                    "sessionId": state.session_id,
                    "interviewId": state.interview_id,
                    "violationCount": state.violation_count,
                    "eventCount": state.event_count,
                    "lastEvent": state.last_event,
                    "uptimeSeconds": state.start_time.map(|t| (Utc::now() - t).num_seconds()).unwrap_or(0),
                }))) {
                    Ok(v) => IpcResponse::Ok(v),
                    Err(e) => IpcResponse::Error { code: "STATUS_ERROR".into(), message: e.to_string() },
                }
            }
        }
    }
}

pub async fn run(running: Arc<AtomicBool>) -> Result<()> {
    let server = IpcServer::bind().await?;
    server.write_port_file()?;
    tracing::info!(port = server.port(), "IPC server listening");

    let service = Arc::new(ServiceState::new()?);

    while running.load(Ordering::SeqCst) {
        let mut conn = match tokio::time::timeout(
            tokio::time::Duration::from_secs(1),
            server.accept(),
        )
        .await
        {
            Ok(Ok(conn)) => conn,
            Ok(Err(e)) => {
                tracing::error!(?e, "accept error");
                continue;
            }
            Err(_) => continue,
        };

        let svc = Arc::clone(&service);
        tokio::spawn(async move {
            let addr = conn.addr;
            tracing::info!(%addr, "UI client connected");

            loop {
                match conn.read_request().await {
                    Ok(request) => {
                        tracing::debug!(?request, "request received");
                        let response = svc.handle_request(request).await;
                        if let Err(e) = conn.send_response(&response).await {
                            tracing::error!(?e, "failed to send response");
                            break;
                        }
                    }
                    Err(e) => {
                        tracing::info!(%addr, ?e, "UI client disconnected");
                        break;
                    }
                }
            }
        });
    }

    Ok(())
}
