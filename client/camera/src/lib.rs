use async_trait::async_trait;
use serde_json::json;
use telemetry::{TelemetryCollector, TelemetryError, TelemetryEvent, TelemetryKind};
use uuid::Uuid;

pub struct CameraConsentCollector;

#[async_trait]
impl TelemetryCollector for CameraConsentCollector {
    fn name(&self) -> &'static str {
        "camera"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Video,
            payload: json!({
                "cameraAvailable": true,
                "activeDevice": "Integrated Camera",
                "summary": "Camera ready"
            }),
        }])
    }
}
