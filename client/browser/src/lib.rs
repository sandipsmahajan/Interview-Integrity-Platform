use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
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
