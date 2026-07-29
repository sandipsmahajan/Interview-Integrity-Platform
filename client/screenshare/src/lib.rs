use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DisplayConfiguration {
    pub monitor_count: u8,
    pub primary_resolution: String,
}

impl DisplayConfiguration {
    pub fn violates_single_monitor_policy(&self) -> bool {
        self.monitor_count > 1
    }
}
