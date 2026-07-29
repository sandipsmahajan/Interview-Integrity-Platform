use async_trait::async_trait;
use serde_json::json;
use telemetry::{TelemetryCollector, TelemetryError, TelemetryEvent, TelemetryKind};
use uuid::Uuid;

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
                "authorized": true
            }),
        }])
    }
}
