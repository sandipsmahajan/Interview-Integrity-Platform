use serde::{Deserialize, Serialize};

/// Remote configuration downloaded from the backend after authentication.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FeatureFlags {
    pub hide_telemetry_panel: bool,
    pub enable_telemetry_streaming: bool,
    pub enable_device_summary: bool,
    pub enable_analytics: bool,
    pub enable_debug_mode: bool,
    pub enable_diagnostics: bool,
    pub enable_heartbeat: bool,
    pub enable_network_collector: bool,
    pub enable_display_collector: bool,
    pub enable_audio_collector: bool,
    pub enable_video_collector: bool,
    pub enable_window_focus_collector: bool,
    pub enable_system_health_collector: bool,
}

impl Default for FeatureFlags {
    fn default() -> Self {
        Self {
            hide_telemetry_panel: false,
            enable_telemetry_streaming: true,
            enable_device_summary: true,
            enable_analytics: false,
            enable_debug_mode: false,
            enable_diagnostics: true,
            enable_heartbeat: true,
            enable_network_collector: true,
            enable_display_collector: true,
            enable_audio_collector: true,
            enable_video_collector: true,
            enable_window_focus_collector: true,
            enable_system_health_collector: true,
        }
    }
}

/// Per-collector enablement derived from feature flags.
#[derive(Debug, Clone)]
pub struct CollectorConfig {
    pub flags: FeatureFlags,
}

impl CollectorConfig {
    pub fn new(flags: FeatureFlags) -> Self {
        Self { flags }
    }

    pub fn is_enabled(&self, collector: &str) -> bool {
        match collector {
            "heartbeat" => self.flags.enable_heartbeat,
            "system_summary" | "system_health" => self.flags.enable_system_health_collector,
            "network" => self.flags.enable_network_collector,
            "display" => self.flags.enable_display_collector,
            "camera" | "microphone" => {
                self.flags.enable_video_collector || self.flags.enable_audio_collector
            }
            "window_focus" => self.flags.enable_window_focus_collector,
            _ => self.flags.enable_diagnostics,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ClientSettings {
    pub theme: Theme,
    pub language: String,
}

impl Default for ClientSettings {
    fn default() -> Self {
        Self {
            theme: Theme::Dark,
            language: "en".into(),
        }
    }
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum Theme {
    Dark,
    Light,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteConfig {
    pub feature_flags: FeatureFlags,
    pub organization_name: String,
    pub support_email: String,
    pub data_retention_days: u32,
    pub privacy_notice_url: String,
    pub terms_url: String,
}

impl Default for RemoteConfig {
    fn default() -> Self {
        Self {
            feature_flags: FeatureFlags::default(),
            organization_name: "Integrity Pro".into(),
            support_email: "support@integritypro.com".into(),
            data_retention_days: 90,
            privacy_notice_url: "https://integritypro.com/privacy".into(),
            terms_url: "https://integritypro.com/terms".into(),
        }
    }
}

/// Extension point for future enterprise policy evaluation modules.
pub trait EnterprisePolicyModule: Send + Sync {
    fn id(&self) -> &str;
    fn evaluate(&self, context: &serde_json::Value) -> Vec<String>;
}

pub struct PolicyModuleRegistry {
    modules: Vec<Box<dyn EnterprisePolicyModule>>,
}

impl PolicyModuleRegistry {
    pub fn new() -> Self {
        Self { modules: Vec::new() }
    }

    pub fn register(&mut self, module: Box<dyn EnterprisePolicyModule>) {
        self.modules.push(module);
    }

    pub fn evaluate_all(&self, context: &serde_json::Value) -> Vec<String> {
        self.modules
            .iter()
            .flat_map(|module| module.evaluate(context))
            .collect()
    }
}

impl Default for PolicyModuleRegistry {
    fn default() -> Self {
        Self::new()
    }
}
