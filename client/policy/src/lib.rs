use serde::{Deserialize, Serialize};
use telemetry::{TelemetryEvent, TelemetryKind};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum Severity {
    Info,
    Low,
    Medium,
    High,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PolicyRule {
    pub code: String,
    pub enabled: bool,
    pub severity: Severity,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PolicySet {
    pub rules: Vec<PolicyRule>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LocalViolation {
    pub rule_code: String,
    pub severity: Severity,
    pub message: String,
}

pub struct PolicyEngine {
    policy_set: PolicySet,
}

impl PolicyEngine {
    pub fn new(policy_set: PolicySet) -> Self {
        Self { policy_set }
    }

    pub fn evaluate(&self, event: &TelemetryEvent) -> Vec<LocalViolation> {
        let mut violations = Vec::new();

        match event.kind {
            TelemetryKind::Browser => {
                if event.payload.get("outOfFocus").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "BROWSER_FOCUS_LOST", "Interview browser lost focus");
                }
                if event.payload.get("devtoolsOpened").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "DEVTOOLS_DETECTED", "Browser developer tools were opened");
                }
            }
            TelemetryKind::Process => {
                if let Some(processes) = event.payload.get("processes").and_then(|v| v.as_array()) {
                    for proc in processes {
                        let name = proc.get("name").and_then(|v| v.as_str()).unwrap_or("");
                        let lower = name.to_lowercase();
                        if is_screen_recorder(&lower) {
                            self.push_if_enabled(&mut violations, "SCREEN_RECORDER_DETECTED",
                                &format!("Potential screen recording software detected: {}", name));
                        }
                        if is_remote_desktop(&lower) {
                            self.push_if_enabled(&mut violations, "REMOTE_DESKTOP_DETECTED",
                                &format!("Remote desktop software detected: {}", name));
                        }
                        if is_ai_assistant(&lower) {
                            self.push_if_enabled(&mut violations, "AI_ASSISTANT_DETECTED",
                                &format!("Potential AI interview assistant detected: {}", name));
                        }
                    }
                }
            }
            TelemetryKind::WindowFocus => {
                if event.payload.get("inFocus").and_then(|v| v.as_bool()) == Some(false) {
                    self.push_if_enabled(&mut violations, "WINDOW_FOCUS_LOST", "Application window lost focus during interview");
                }
            }
            TelemetryKind::Display => {
                if let Some(count) = event.payload.get("monitorCount").and_then(|v| v.as_u64()) {
                    if count > 1 {
                        self.push_if_enabled(&mut violations, "MULTIPLE_MONITORS", "Multiple monitors detected during interview");
                    }
                }
            }
            TelemetryKind::SystemHealth => {
                if let Some(cpu) = event.payload.get("cpuUsagePercent").and_then(|v| v.as_f64()) {
                    if cpu > 95.0 {
                        self.push_if_enabled(&mut violations, "HIGH_CPU_USAGE", "CPU usage exceeded 95% threshold");
                    }
                }
                if let Some(mem) = event.payload.get("memoryUsagePercent").and_then(|v| v.as_f64()) {
                    if mem > 90.0 {
                        self.push_if_enabled(&mut violations, "HIGH_MEMORY_USAGE", "Memory usage exceeded 90% threshold");
                    }
                }
            }
            TelemetryKind::Device => {
                if event.payload.get("virtualMachineDetected").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "VM_DETECTED", "Virtual machine indicators were detected");
                }
            }
            TelemetryKind::OverlayDetection => {
                if let Some(count) = event.payload.get("overlayCount").and_then(|v| v.as_u64()) {
                    if count > 0 {
                        self.push_if_enabled(&mut violations, "OVERLAY_DETECTED",
                            &format!("{} suspicious overlay windows detected", count));
                    }
                }
            }
            TelemetryKind::Clipboard => {
                if event.payload.get("clipboardChanged").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "CLIPBOARD_ACCESS", "Clipboard content was modified during interview");
                }
            }
            TelemetryKind::FullscreenDetection => {
                if event.payload.get("isFullscreen").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "FULLSCREEN_DETECTED", "Fullscreen window detected - possible screen takeover");
                }
            }
            TelemetryKind::IdleDetection => {
                if let Some(idle) = event.payload.get("idleSeconds").and_then(|v| v.as_u64()) {
                    if idle > 600 {
                        self.push_if_enabled(&mut violations, "EXTENDED_IDLE",
                            &format!("User idle for {} seconds", idle));
                    }
                }
            }
            TelemetryKind::LockScreen => {
                if event.payload.get("sessionLocked").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "SESSION_LOCKED", "Workstation was locked during interview");
                }
            }
            TelemetryKind::VpnDetection => {
                if event.payload.get("vpnDetected").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "VPN_DETECTED", "VPN or proxy connection detected");
                }
            }
            TelemetryKind::CameraDevice => {
                if event.payload.get("virtualCameraDetected").and_then(|v| v.as_bool()) == Some(true) {
                    self.push_if_enabled(&mut violations, "VIRTUAL_CAMERA", "Virtual camera device detected");
                }
            }
            TelemetryKind::AudioDevice => {
                if let Some(count) = event.payload.get("deviceCount").and_then(|v| v.as_u64()) {
                    if count > 3 {
                        self.push_if_enabled(&mut violations, "MULTIPLE_AUDIO_DEVICES",
                            "Unusually high number of audio devices detected");
                    }
                }
            }
            _ => {}
        }

        violations
    }

    fn push_if_enabled(&self, target: &mut Vec<LocalViolation>, code: &str, message: &str) {
        if let Some(rule) = self.policy_set.rules.iter().find(|r| r.code == code && r.enabled) {
            target.push(LocalViolation {
                rule_code: code.to_string(),
                severity: rule.severity.clone(),
                message: message.to_string(),
            });
        }
    }
}

pub fn default_policy_set() -> PolicySet {
    PolicySet {
        rules: vec![
            rule("BROWSER_FOCUS_LOST", Severity::Medium, true),
            rule("DEVTOOLS_DETECTED", Severity::High, true),
            rule("SCREEN_RECORDER_DETECTED", Severity::Critical, true),
            rule("REMOTE_DESKTOP_DETECTED", Severity::Critical, true),
            rule("AI_ASSISTANT_DETECTED", Severity::Critical, true),
            rule("WINDOW_FOCUS_LOST", Severity::Low, true),
            rule("MULTIPLE_MONITORS", Severity::Low, true),
            rule("VM_DETECTED", Severity::High, true),
            rule("HIGH_CPU_USAGE", Severity::Info, true),
            rule("HIGH_MEMORY_USAGE", Severity::Info, true),
            rule("OVERLAY_DETECTED", Severity::High, true),
            rule("CLIPBOARD_ACCESS", Severity::High, true),
            rule("FULLSCREEN_DETECTED", Severity::Medium, true),
            rule("EXTENDED_IDLE", Severity::Low, true),
            rule("SESSION_LOCKED", Severity::High, true),
            rule("VPN_DETECTED", Severity::Medium, true),
            rule("VIRTUAL_CAMERA", Severity::High, true),
            rule("MULTIPLE_AUDIO_DEVICES", Severity::Low, true),
        ],
    }
}

fn rule(code: &str, severity: Severity, enabled: bool) -> PolicyRule {
    PolicyRule {
        code: code.to_string(),
        enabled,
        severity,
    }
}

fn is_screen_recorder(name: &str) -> bool {
    let keywords = [
        "obs", "streamlabs", "xsplit", "bandicam", "fraps", "action!",
        "dxtory", "screenrec", "screencast", "camtasia", "snagit",
        "loom", "sharex", "greenshot", "lightshot",
    ];
    keywords.iter().any(|k| name.contains(k))
}

fn is_remote_desktop(name: &str) -> bool {
    let keywords = [
        "teamviewer", "anydesk", "vnc", "rdp", "mstsc", "logmein",
        "splashtop", "gotomypc", "chrome remote desktop", "nomachine",
        "tightvnc", "ultravnc", "remmina",
    ];
    keywords.iter().any(|k| name.contains(k))
}

fn is_ai_assistant(name: &str) -> bool {
    let keywords = [
        "chatgpt", "copilot", "bard", "claude", "perplexity",
        "phind", "codeium", "tabnine", "kite", "interview",
    ];
    keywords.iter().any(|k| name.contains(k))
}
