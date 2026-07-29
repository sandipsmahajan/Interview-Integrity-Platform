use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UpdateManifest {
    pub version: String,
    pub url: String,
    pub sha256: String,
}

pub fn update_required(current: &str, manifest: &UpdateManifest) -> bool {
    current != manifest.version
}
