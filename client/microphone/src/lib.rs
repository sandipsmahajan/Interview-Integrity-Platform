use async_trait::async_trait;
use serde_json::json;
use telemetry::{TelemetryCollector, TelemetryError, TelemetryEvent, TelemetryKind};
use uuid::Uuid;

pub struct MicrophoneConsentCollector;

#[async_trait]
impl TelemetryCollector for MicrophoneConsentCollector {
    fn name(&self) -> &'static str { "microphone" }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent { session_id, kind: TelemetryKind::Audio, payload: json!({"microphoneEnabled": true}) }])
    }
}
