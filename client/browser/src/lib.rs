use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BrowserPolicy {
    pub allowed_domains: Vec<String>,
    pub disable_devtools: bool,
    pub disable_downloads: bool,
    pub disable_extensions: bool,
    pub disable_printing: bool,
    pub block_popups: bool,
}

impl BrowserPolicy {
    pub fn enterprise_default() -> Self {
        Self {
            allowed_domains: vec![
                "teams.microsoft.com".into(),
                "meet.google.com".into(),
                "zoom.us".into(),
                "daily.co".into(),
                "livekit.io".into(),
                "webrtc".into(),
            ],
            disable_devtools: true,
            disable_downloads: true,
            disable_extensions: true,
            disable_printing: true,
            block_popups: true,
        }
    }

    pub fn allows(&self, url: &str) -> bool {
        self.allowed_domains.iter().any(|domain| url.contains(domain))
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct BrowserStatus {
    pub connected: bool,
    pub current_url: Option<String>,
    pub in_focus: bool,
    pub meeting_duration_secs: u64,
}

pub struct BrowserController {
    policy: BrowserPolicy,
    status: BrowserStatus,
}

impl BrowserController {
    pub fn new(policy: BrowserPolicy) -> Self {
        Self {
            policy,
            status: BrowserStatus {
                connected: false,
                current_url: None,
                in_focus: true,
                meeting_duration_secs: 0,
            },
        }
    }

    pub fn policy(&self) -> &BrowserPolicy {
        &self.policy
    }

    pub fn status(&self) -> &BrowserStatus {
        &self.status
    }

    pub fn navigate(&mut self, url: &str) -> Result<(), String> {
        if !self.policy.allows(url) {
            return Err(format!("Navigation blocked: {url} is not in the approved domain list"));
        }
        self.status.connected = true;
        self.status.current_url = Some(url.to_string());
        Ok(())
    }

    pub fn set_focus(&mut self, in_focus: bool) {
        self.status.in_focus = in_focus;
    }

    pub fn tick_duration(&mut self) {
        if self.status.connected {
            self.status.meeting_duration_secs += 1;
        }
    }
}
