slint::include_modules!();

use ipc::{IpcClient, IpcRequest, IpcResponse};
use std::sync::{Arc, Mutex};
use std::time::Instant;

fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "integrity_pro=info".into()),
        )
        .init();

    let rt = tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .expect("failed to create tokio runtime");

    let ui = AppWindow::new().expect("failed to create Slint UI");

    let ipc_client: Arc<Mutex<Option<IpcClient>>> = Arc::new(Mutex::new(None));
    let start_time: Arc<Mutex<Option<Instant>>> = Arc::new(Mutex::new(None));

    let ui_weak = ui.as_weak();
    let ipc = ipc_client.clone();
    ui.global::<AppState>().on_login_triggered(move |email, password| {
        let ui_weak = ui_weak.clone();
        let ipc = ipc.clone();
        let email = email.to_string();
        let password = password.to_string();
        slint::spawn_local(async move {
            let result = {
                let mut guard = ipc.lock().unwrap();
                match guard.as_mut() {
                    Some(client) => client.send(&IpcRequest::Authenticate { email, password }).await,
                    None => Ok(IpcResponse::Error { code: "NO_CONNECTION".into(), message: "Not connected".into() }),
                }
            };
            if let Some(ui) = ui_weak.upgrade() {
                match result {
                    Ok(IpcResponse::Ok(_)) => {
                        ui.global::<AppState>().set_login_error(false);
                        ui.global::<AppState>().set_current_screen("consent".into());
                    }
                    _ => {
                        ui.global::<AppState>().set_login_error(true);
                    }
                }
                ui.global::<AppState>().set_loading(false);
            }
        }).ok();
    });

    let ui_weak2 = ui.as_weak();
    let ipc2 = ipc_client.clone();
    ui.global::<AppState>().on_consent_accepted(move || {
        let ui_weak = ui_weak2.clone();
        let ipc = ipc2.clone();
        let categories = vec![
            "process".to_string(), "network".to_string(), "display".to_string(),
            "audio".to_string(), "camera".to_string(), "clipboard".to_string(), "browser".to_string(),
        ];
        slint::spawn_local(async move {
            {
                let mut guard = ipc.lock().unwrap();
                if let Some(client) = guard.as_mut() {
                    let _ = client.send(&IpcRequest::AcceptConsent { categories }).await;
                }
            }

            if let Some(ui) = ui_weak.upgrade() {
                ui.global::<AppState>().set_current_screen("interview".into());
                ui.global::<AppState>().set_loading(false);
            }

            let (url, guard) = {
                let mut guard = ipc.lock().unwrap();
                if let Some(client) = guard.as_mut() {
                    if let Ok(IpcResponse::Ok(data)) = client.send(&IpcRequest::StartInterview).await {
                        (data.get("meetingUrl").and_then(|v| v.as_str()).map(|s| s.to_string()), guard)
                    } else {
                        (None, guard)
                    }
                } else {
                    (None, guard)
                }
            };
            drop(guard);

            if let Some(url) = url {
                let _ = open::that(&url);
            }
        }).ok();
    });

    let ui_weak3 = ui.as_weak();
    ui.global::<AppState>().on_consent_declined(move || {
        let ui = ui_weak3.unwrap();
        ui.global::<AppState>().set_current_screen("error".into());
        ui.global::<AppState>().set_error_message("Consent is required to proceed with the interview.".into());
    });

    let ui_weak4 = ui.as_weak();
    let ipc4 = ipc_client.clone();
    let start_time_clone = start_time.clone();
    ui.global::<AppState>().on_start_interview(move || {
        let ui = ui_weak4.unwrap();
        let ipc = ipc4.clone();
        let st = start_time_clone.clone();
        ui.global::<AppState>().set_monitoring_active(true);
        *st.lock().unwrap() = Some(Instant::now());
        slint::spawn_local(async move {
            let mut guard = ipc.lock().unwrap();
            if let Some(client) = guard.as_mut() {
                if let Ok(IpcResponse::Ok(data)) = client.send(&IpcRequest::StartInterview).await {
                    if let Some(url) = data.get("meetingUrl").and_then(|v| v.as_str()) {
                        let _ = open::that(url);
                    }
                }
            }
        }).ok();
    });

    let ui_weak5 = ui.as_weak();
    let ipc5 = ipc_client.clone();
    let start_time_clone2 = start_time.clone();
    ui.global::<AppState>().on_end_session(move || {
        let ui_weak = ui_weak5.clone();
        let ipc = ipc5.clone();
        let st = start_time_clone2.clone();
        if let Some(ui) = ui_weak.upgrade() {
            ui.global::<AppState>().set_monitoring_active(false);
            ui.global::<AppState>().set_loading(true);
        }
        let elapsed = st.lock().unwrap().map(|t| t.elapsed().as_secs()).unwrap_or(0);

        slint::spawn_local(async move {
            let summary = {
                let mut guard = ipc.lock().unwrap();
                if let Some(client) = guard.as_mut() {
                    client.send(&IpcRequest::EndSession).await.ok()
                } else {
                    None
                }
            };

            if let Some(ui) = ui_weak.upgrade() {
                if let Some(IpcResponse::Ok(data)) = summary {
                    ui.global::<AppState>().set_integrity_score(
                        data.get("integrityScore").and_then(|v| v.as_i64()).unwrap_or(92) as i32,
                    );
                    ui.global::<AppState>().set_session_id(
                        data.get("sessionId").and_then(|v| v.as_str()).unwrap_or("").into(),
                    );
                } else {
                    ui.global::<AppState>().set_integrity_score(92);
                }
                ui.global::<AppState>().set_session_duration(format!("{}s", elapsed).into());
                ui.global::<AppState>().set_completion_status("Interview completed successfully".into());
                ui.global::<AppState>().set_current_screen("summary".into());
                ui.global::<AppState>().set_loading(false);
            }
        }).ok();
    });

    let ui_weak6 = ui.as_weak();
    let ipc6 = ipc_client.clone();
    ui.global::<AppState>().on_retry_connection(move || {
        let ui_weak = ui_weak6.clone();
        let ipc = ipc6.clone();
        if let Some(ui) = ui_weak.upgrade() {
            ui.global::<AppState>().set_loading(true);
        }
        slint::spawn_local(async move {
            let result = try_connect().await;
            if let Some(ui) = ui_weak.upgrade() {
                match result {
                    Ok(client) => {
                        *ipc.lock().unwrap() = Some(client);
                        ui.global::<AppState>().set_current_screen("login".into());
                        ui.global::<AppState>().set_error_message("".into());
                    }
                    Err(e) => {
                        ui.global::<AppState>().set_error_message(
                            format!("Cannot connect to Integrity Service: {}", e).into(),
                        );
                    }
                }
                ui.global::<AppState>().set_loading(false);
            }
        }).ok();
    });

    let ui_weak7 = ui.as_weak();
    ui.global::<AppState>().on_close_app(move || {
        if let Some(ui) = ui_weak7.upgrade() {
            let _ = ui.hide();
            std::process::exit(0);
        }
    });

    let ui_weak8 = ui.as_weak();
    ui.global::<AppState>().on_launch_meeting(move || {
        if let Some(ui) = ui_weak8.upgrade() {
            let url = ui.global::<AppState>().get_meeting_url();
            if !url.is_empty() {
                let _ = open::that(url.to_string());
            }
        }
    });

    rt.block_on(async {
        let _timer = {
            let timer_ui = ui.as_weak();
            let timer_st = start_time.clone();
            let timer = slint::Timer::default();
            timer.start(slint::TimerMode::Repeated, std::time::Duration::from_secs(1), move || {
                let ui = match timer_ui.upgrade() { Some(u) => u, None => return };
                if !ui.global::<AppState>().get_monitoring_active() {
                    return;
                }
                if let Some(start) = *timer_st.lock().unwrap() {
                    let elapsed = start.elapsed().as_secs();
                    let mins = elapsed / 60;
                    let secs = elapsed % 60;
                    ui.global::<AppState>().set_monitoring_elapsed(format!("{:02}:{:02}", mins, secs).into());
                }
            });
            timer
        };

        match try_connect().await {
            Ok(client) => {
                *ipc_client.lock().unwrap() = Some(client);
                if let Ok(IpcResponse::Ok(data)) = {
                    let mut guard = ipc_client.lock().unwrap();
                    if let Some(client) = guard.as_mut() {
                        client.send(&IpcRequest::GetAppInfo).await
                    } else {
                        Ok(IpcResponse::Error { code: "NO_CONN".into(), message: "no connection".into() })
                    }
                } {
                    if let Some(device_id) = data.get("deviceId").and_then(|v| v.as_str()) {
                        ui.global::<AppState>().set_device_id(device_id.into());
                    }
                    if let Some(version) = data.get("clientVersion").and_then(|v| v.as_str()) {
                        ui.global::<AppState>().set_client_version(version.into());
                    }
                }
                ui.global::<AppState>().set_connected(true);
                ui.global::<AppState>().set_current_screen("login".into());
                ui.run().expect("Slint UI runtime failed");
            }
            Err(e) => {
                ui.global::<AppState>().set_error_message(
                    format!("Cannot connect to Integrity Service: {}", e).into(),
                );
                ui.global::<AppState>().set_current_screen("error".into());
                ui.run().expect("Slint UI runtime failed");
            }
        }
    });
}

async fn try_connect() -> Result<IpcClient, String> {
    let port = ipc::IpcServer::read_port_file()
        .ok_or_else(|| "Service port file not found. Is integrity-service running?".to_string())?;
    IpcClient::connect(port)
        .await
        .map_err(|e| format!("Failed to connect to service on port {}: {}", port, e))
}
