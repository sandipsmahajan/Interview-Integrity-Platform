mod commands;
mod state;

use state::AppState;
use tauri::Manager;

fn main() {
    logger::init();
    let app_state = AppState::new().expect("failed to initialize application state");

    tauri::Builder::default()
        .manage(app_state)
        .invoke_handler(tauri::generate_handler![
            commands::get_launch_context,
            commands::authenticate,
            commands::get_system_checks,
            commands::get_remote_config,
            commands::get_interview,
            commands::accept_consent,
            commands::decline_consent,
            commands::start_interview,
            commands::end_session,
            commands::browser_policy,
            commands::validate_navigation,
            commands::get_settings,
            commands::update_settings,
            commands::get_feature_flags,
            commands::get_app_info,
        ])
        .setup(|app| {
            let handle = app.handle().clone();
            let state = app.state::<AppState>();
            state.start_panel_event_bridge(handle);
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("failed to run Integrity Pro desktop client");
}
