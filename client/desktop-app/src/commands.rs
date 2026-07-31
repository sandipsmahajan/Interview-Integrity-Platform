use crate::state::AppState;
use browser::BrowserPolicy;
use config::{ClientSettings, FeatureFlags, RemoteConfig};
use network::{AuthResponse, InterviewContext};
use system::SystemCheck;
use tauri::State;

#[tauri::command]
pub fn get_launch_context(state: State<'_, AppState>, args: Vec<String>) -> Result<crate::state::LaunchContext, String> {
    state.launch_context(&args).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn authenticate(
    state: State<'_, AppState>,
    email: String,
    password: String,
) -> Result<AuthResponse, String> {
    state.authenticate(email, password).await.map_err(|e| e.to_string())
}

#[tauri::command]
pub fn get_system_checks(state: State<'_, AppState>) -> Result<Vec<SystemCheck>, String> {
    state.system_checks().map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_remote_config(state: State<'_, AppState>) -> Result<RemoteConfig, String> {
    state.load_remote_config().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_interview(state: State<'_, AppState>) -> Result<InterviewContext, String> {
    state.load_interview().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub fn accept_consent(state: State<'_, AppState>, categories: Vec<String>) -> Result<(), String> {
    state.accept_consent(categories).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn decline_consent(state: State<'_, AppState>) -> Result<(), String> {
    state.decline_consent().map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn start_interview(state: State<'_, AppState>) -> Result<InterviewContext, String> {
    state.start_interview().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn end_session(state: State<'_, AppState>) -> Result<serde_json::Value, String> {
    state.end_session().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub fn browser_policy(state: State<'_, AppState>) -> Result<BrowserPolicy, String> {
    state.browser_policy().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn validate_navigation(state: State<'_, AppState>, url: String) -> Result<bool, String> {
    state.validate_navigation(url).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn get_settings(state: State<'_, AppState>) -> Result<ClientSettings, String> {
    state.settings().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn update_settings(state: State<'_, AppState>, settings: ClientSettings) -> Result<ClientSettings, String> {
    state.update_settings(settings).map_err(|e| e.to_string())
}

#[tauri::command]
pub fn get_feature_flags(state: State<'_, AppState>) -> Result<FeatureFlags, String> {
    state.feature_flags().map_err(|e| e.to_string())
}

#[tauri::command]
pub fn get_app_info(state: State<'_, AppState>) -> Result<crate::state::AppInfo, String> {
    state.app_info().map_err(|e| e.to_string())
}
