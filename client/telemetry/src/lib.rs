use async_trait::async_trait;
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum TelemetryKind {
    Heartbeat,
    Device,
    Display,
    WindowFocus,
    Process,
    Network,
    Audio,
    Video,
    Browser,
    Crash,
    Lifecycle,
    SystemHealth,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TelemetryEvent {
    pub session_id: Uuid,
    pub kind: TelemetryKind,
    pub payload: Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TelemetryPanelEvent {
    pub timestamp: String,
    pub message: String,
    pub kind: String,
    pub status: PanelStatus,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum PanelStatus {
    Info,
    Success,
    Warning,
    Error,
}

impl TelemetryEvent {
    pub fn to_panel_event(&self) -> TelemetryPanelEvent {
        let message = match self.kind {
            TelemetryKind::Heartbeat => "Heartbeat".into(),
            TelemetryKind::Device => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Device information updated")
                .into(),
            TelemetryKind::Display => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Display configuration updated")
                .into(),
            TelemetryKind::WindowFocus => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Window focus changed")
                .into(),
            TelemetryKind::Network => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Network quality updated")
                .into(),
            TelemetryKind::Audio => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Audio device updated")
                .into(),
            TelemetryKind::Video => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Video device updated")
                .into(),
            TelemetryKind::Browser => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Browser status updated")
                .into(),
            TelemetryKind::Lifecycle => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("Application lifecycle event")
                .into(),
            TelemetryKind::SystemHealth => self
                .payload
                .get("summary")
                .and_then(|v| v.as_str())
                .unwrap_or("System health updated")
                .into(),
            TelemetryKind::Crash => "Application crash detected".into(),
            TelemetryKind::Process => "Process activity detected".into(),
        };

        let status = match self.kind {
            TelemetryKind::Crash => PanelStatus::Error,
            TelemetryKind::Network => {
                if self.payload.get("quality").and_then(|v| v.as_str()) == Some("poor") {
                    PanelStatus::Warning
                } else {
                    PanelStatus::Success
                }
            }
            TelemetryKind::Heartbeat | TelemetryKind::Lifecycle => PanelStatus::Info,
            _ => PanelStatus::Success,
        };

        TelemetryPanelEvent {
            timestamp: Utc::now().format("%H:%M:%S").to_string(),
            message,
            kind: format!("{:?}", self.kind),
            status,
        }
    }
}

#[derive(Debug, thiserror::Error)]
pub enum TelemetryError {
    #[error("collector failed: {0}")]
    Collector(String),
}

#[async_trait]
pub trait TelemetryCollector: Send + Sync {
    fn name(&self) -> &'static str;
    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError>;
}

pub struct CollectorRegistry {
    collectors: Vec<Box<dyn TelemetryCollector>>,
}

impl CollectorRegistry {
    pub fn new(collectors: Vec<Box<dyn TelemetryCollector>>) -> Self {
        Self { collectors }
    }

    pub fn empty() -> Self {
        Self { collectors: Vec::new() }
    }

    pub fn register(&mut self, collector: Box<dyn TelemetryCollector>) {
        self.collectors.push(collector);
    }

    pub async fn collect_all(&self, session_id: Uuid) -> Vec<Result<Vec<TelemetryEvent>, TelemetryError>> {
        let mut results = Vec::with_capacity(self.collectors.len());
        for collector in &self.collectors {
            results.push(collector.collect(session_id).await);
        }
        results
    }
}

pub struct TelemetryPipeline;

impl TelemetryPipeline {
    pub fn panel_events(events: &[TelemetryEvent]) -> Vec<TelemetryPanelEvent> {
        events.iter().map(TelemetryEvent::to_panel_event).collect()
    }
}
