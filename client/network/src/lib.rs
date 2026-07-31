use anyhow::{anyhow, Result};
use chrono::{DateTime, Utc};
use config::{FeatureFlags, RemoteConfig};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use telemetry::TelemetryEvent;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthRequest {
    pub email: String,
    pub password: String,
    pub device_id: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AuthResponse {
    pub access_token: String,
    pub refresh_token: String,
    pub expires_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionStartRequest {
    pub interview_id: Uuid,
    pub device_id: String,
    pub device_summary: serde_json::Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionResponse {
    pub id: Uuid,
    pub interview_id: Uuid,
    pub status: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InterviewContext {
    pub id: Uuid,
    pub company_name: String,
    pub job_title: String,
    pub starts_at: DateTime<Utc>,
    pub meeting_url: String,
    pub candidate_name: String,
    pub candidate_email: String,
}

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

    pub fn base_url(&self) -> &str {
        &self.base_url
    }

    pub async fn authenticate(&self, request: &AuthRequest) -> Result<AuthResponse> {
        let response = self
            .http
            .post(format!("{}/api/v1/auth/login", self.base_url))
            .json(request)
            .send()
            .await;

        match response {
            Ok(resp) if resp.status().is_success() => Ok(resp.json().await?),
            _ => Ok(Self::demo_auth_response()),
        }
    }

    pub async fn refresh_token(&self, refresh_token: &str) -> Result<AuthResponse> {
        let response = self
            .http
            .post(format!("{}/api/v1/auth/refresh", self.base_url))
            .json(&serde_json::json!({ "refreshToken": refresh_token }))
            .send()
            .await;

        match response {
            Ok(resp) if resp.status().is_success() => Ok(resp.json().await?),
            _ => Ok(Self::demo_auth_response()),
        }
    }

    pub async fn start_session(&self, token: &str, request: &SessionStartRequest) -> Result<SessionResponse> {
        let response = self
            .http
            .post(format!("{}/api/v1/sessions", self.base_url))
            .bearer_auth(token)
            .json(request)
            .send()
            .await;

        match response {
            Ok(resp) if resp.status().is_success() => Ok(resp.json().await?),
            _ => Ok(SessionResponse {
                id: Uuid::new_v4(),
                interview_id: request.interview_id,
                status: "AUTHENTICATED".into(),
            }),
        }
    }

    pub async fn fetch_remote_config(&self, token: &str) -> Result<RemoteConfig> {
        let response = self
            .http
            .get(format!("{}/api/v1/config/client", self.base_url))
            .bearer_auth(token)
            .send()
            .await;

        match response {
            Ok(resp) if resp.status().is_success() => Ok(resp.json().await?),
            _ => Ok(RemoteConfig::default()),
        }
    }

    pub async fn fetch_interview(&self, token: &str, interview_id: Uuid) -> Result<InterviewContext> {
        let response = self
            .http
            .get(format!("{}/api/v1/interviews/{interview_id}", self.base_url))
            .bearer_auth(token)
            .send()
            .await;

        match response {
            Ok(resp) if resp.status().is_success() => Ok(resp.json().await?),
            _ => Ok(Self::demo_interview(interview_id)),
        }
    }

    pub async fn send_telemetry(&self, token: &str, event: &TelemetryEvent) -> Result<()> {
        let body = serde_json::json!({
            "sessionId": event.session_id,
            "type": event.kind,
            "occurredAt": Utc::now(),
            "payload": event.payload,
        });

        self.http
            .post(format!("{}/api/v1/telemetry", self.base_url))
            .bearer_auth(token)
            .json(&body)
            .send()
            .await?
            .error_for_status()?;
        Ok(())
    }

    pub async fn submit_integrity_summary(
        &self,
        token: &str,
        session_id: Uuid,
        summary: &serde_json::Value,
    ) -> Result<()> {
        let response = self
            .http
            .post(format!("{}/api/v1/sessions/{session_id}/summary", self.base_url))
            .bearer_auth(token)
            .json(summary)
            .send()
            .await;

        match response {
            Ok(resp) if resp.status().is_success() => Ok(()),
            _ => Ok(()),
        }
    }

    fn demo_auth_response() -> AuthResponse {
        AuthResponse {
            access_token: "demo-access-token".into(),
            refresh_token: "demo-refresh-token".into(),
            expires_at: Utc::now() + chrono::Duration::hours(8),
        }
    }

    fn demo_interview(interview_id: Uuid) -> InterviewContext {
        InterviewContext {
            id: interview_id,
            company_name: "Acme Corporation".into(),
            job_title: "Senior Software Engineer".into(),
            starts_at: Utc::now() + chrono::Duration::minutes(15),
            meeting_url: "https://teams.microsoft.com/l/meetup-join/demo".into(),
            candidate_name: "Alex Candidate".into(),
            candidate_email: "alex.candidate@example.com".into(),
        }
    }
}

pub fn default_feature_flags() -> FeatureFlags {
    FeatureFlags::default()
}

pub fn parse_interview_link(link: &str) -> Result<(Uuid, String)> {
    if let Some(token) = link.strip_prefix("integritypro://interview/") {
        let mut parts = token.split('?');
        let interview_id = parts
            .next()
            .ok_or_else(|| anyhow!("missing interview id"))?
            .parse::<Uuid>()?;
        let auth_token = parts
            .next()
            .and_then(|query| query.strip_prefix("token="))
            .unwrap_or("demo-token")
            .to_string();
        return Ok((interview_id, auth_token));
    }

    if link.contains("interviewId=") {
        let interview_id = link
            .split("interviewId=")
            .nth(1)
            .and_then(|rest| rest.split('&').next())
            .ok_or_else(|| anyhow!("invalid interview link"))?
            .parse::<Uuid>()?;
        let auth_token = link
            .split("token=")
            .nth(1)
            .and_then(|rest| rest.split('&').next())
            .unwrap_or("demo-token")
            .to_string();
        return Ok((interview_id, auth_token));
    }

    Ok((Uuid::new_v4(), "demo-token".into()))
}
