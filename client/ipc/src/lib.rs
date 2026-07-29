use serde::{Deserialize, Serialize};
use tokio::sync::broadcast;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum IpcMessage {
    Authenticated,
    BrowserReady,
    PolicyUpdated,
    ShutdownRequested,
}

pub fn channel() -> (broadcast::Sender<IpcMessage>, broadcast::Receiver<IpcMessage>) {
    broadcast::channel(128)
}
