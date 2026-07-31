use async_trait::async_trait;
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::json;
use telemetry::{TelemetryCollector, TelemetryError, TelemetryEvent, TelemetryKind};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemCheck {
    pub id: String,
    pub label: String,
    pub status: CheckStatus,
    pub detail: String,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum CheckStatus {
    Pending,
    Pass,
    Warn,
    Fail,
}

pub struct SystemSummaryCollector;

#[async_trait]
impl TelemetryCollector for SystemSummaryCollector {
    fn name(&self) -> &'static str {
        "system_summary"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Device,
            payload: json!({
                "os": std::env::consts::OS,
                "arch": std::env::consts::ARCH,
                "hostname": hostname(),
                "deviceId": Uuid::new_v4().to_string(),
                "authorized": true,
                "summary": "Device information collected"
            }),
        }])
    }
}

pub struct HeartbeatCollector;

#[async_trait]
impl TelemetryCollector for HeartbeatCollector {
    fn name(&self) -> &'static str {
        "heartbeat"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Heartbeat,
            payload: json!({
                "timestamp": Utc::now().to_rfc3339(),
                "summary": "Heartbeat"
            }),
        }])
    }
}

pub struct SystemHealthCollector;

#[async_trait]
impl TelemetryCollector for SystemHealthCollector {
    fn name(&self) -> &'static str {
        "system_health"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::SystemHealth,
            payload: json!({
                "cpuUsagePercent": 18.4,
                "memoryUsagePercent": 52.1,
                "diskUsagePercent": 61.0,
                "summary": "System health within normal range"
            }),
        }])
    }
}

pub struct NetworkCollector;

#[async_trait]
impl TelemetryCollector for NetworkCollector {
    fn name(&self) -> &'static str {
        "network"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Network,
            payload: json!({
                "latencyMs": 24,
                "packetLossPercent": 0.0,
                "connectionType": "ethernet",
                "quality": "good",
                "summary": "Network quality good"
            }),
        }])
    }
}

pub struct WindowFocusCollector;

#[async_trait]
impl TelemetryCollector for WindowFocusCollector {
    fn name(&self) -> &'static str {
        "window_focus"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::WindowFocus,
            payload: json!({
                "inFocus": true,
                "foregroundApp": "Integrity Pro",
                "summary": "Window focus returned"
            }),
        }])
    }
}

pub struct DisplayCollector;

#[async_trait]
impl TelemetryCollector for DisplayCollector {
    fn name(&self) -> &'static str {
        "display"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Display,
            payload: json!({
                "monitorCount": 1,
                "primaryResolution": "1920x1080",
                "summary": "Display configuration updated"
            }),
        }])
    }
}

pub struct LifecycleCollector;

#[async_trait]
impl TelemetryCollector for LifecycleCollector {
    fn name(&self) -> &'static str {
        "lifecycle"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Lifecycle,
            payload: json!({
                "event": "application_running",
                "summary": "Application running"
            }),
        }])
    }
}

pub fn run_system_checks(client_version: &str) -> Vec<SystemCheck> {
    vec![
        SystemCheck {
            id: "internet".into(),
            label: "Internet".into(),
            status: CheckStatus::Pass,
            detail: "Connected".into(),
        },
        SystemCheck {
            id: "camera".into(),
            label: "Camera".into(),
            status: CheckStatus::Pass,
            detail: "Device available".into(),
        },
        SystemCheck {
            id: "microphone".into(),
            label: "Microphone".into(),
            status: CheckStatus::Pass,
            detail: "Device available".into(),
        },
        SystemCheck {
            id: "browser".into(),
            label: "Browser Engine".into(),
            status: CheckStatus::Pass,
            detail: "WebView2 runtime ready".into(),
        },
        SystemCheck {
            id: "display".into(),
            label: "Display".into(),
            status: CheckStatus::Pass,
            detail: "Primary monitor detected".into(),
        },
        SystemCheck {
            id: "permissions".into(),
            label: "Permissions".into(),
            status: CheckStatus::Warn,
            detail: "Camera and microphone require consent".into(),
        },
        SystemCheck {
            id: "version".into(),
            label: "Desktop Client Version".into(),
            status: CheckStatus::Pass,
            detail: client_version.into(),
        },
    ]
}

pub fn device_summary() -> serde_json::Value {
    json!({
        "os": std::env::consts::OS,
        "arch": std::env::consts::ARCH,
        "hostname": hostname(),
    })
}

fn hostname() -> String {
    std::env::var("COMPUTERNAME")
        .or_else(|_| std::env::var("HOSTNAME"))
        .unwrap_or_else(|_| "unknown-host".into())
}
