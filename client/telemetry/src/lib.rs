use async_trait::async_trait;
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
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TelemetryEvent {
    pub session_id: Uuid,
    pub kind: TelemetryKind,
    pub payload: Value,
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

    pub async fn collect_all(&self, session_id: Uuid) -> Vec<Result<Vec<TelemetryEvent>, TelemetryError>> {
        let mut results = Vec::with_capacity(self.collectors.len());
        for collector in &self.collectors {
            results.push(collector.collect(session_id).await);
        }
        results
    }
}
