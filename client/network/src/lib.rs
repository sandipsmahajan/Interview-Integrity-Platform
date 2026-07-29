use anyhow::Result;
use reqwest::Client;
use telemetry::TelemetryEvent;

#[derive(Clone)]
pub struct ApiClient {
    http: Client,
    base_url: String,
}

impl ApiClient {
    pub fn new(base_url: impl Into<String>) -> Self {
        Self {
            http: Client::new(),
            base_url: base_url.into(),
        }
    }

    pub async fn send_telemetry(&self, token: &str, event: &TelemetryEvent) -> Result<()> {
        self.http
            .post(format!("{}/api/v1/telemetry", self.base_url))
            .bearer_auth(token)
            .json(event)
            .send()
            .await?
            .error_for_status()?;
        Ok(())
    }
}
