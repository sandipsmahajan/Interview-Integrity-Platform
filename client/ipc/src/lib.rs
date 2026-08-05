use serde::{Deserialize, Serialize};
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};
use tokio::net::TcpStream;
use uuid::Uuid;

pub const SERVICE_PORT_FILE: &str = ".integrity-pro/.service-port";
pub const IPC_SHARED_SECRET: &str = "integrity-pro-ipc-v1";

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", content = "payload")]
pub enum IpcRequest {
    Ping,
    LaunchContext { args: Vec<String> },
    Authenticate { email: String, password: String },
    GetSystemChecks,
    GetRemoteConfig,
    GetInterview,
    AcceptConsent { categories: Vec<String> },
    DeclineConsent,
    StartInterview,
    EndSession,
    GetSettings,
    UpdateSettings { settings: serde_json::Value },
    GetFeatureFlags,
    GetAppInfo,
    GetStatus,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type", content = "payload")]
pub enum IpcResponse {
    Ok(serde_json::Value),
    Error { code: String, message: String },
    Pong,
    TelemetryEvent(serde_json::Value),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct IpcStatus {
    pub connected: bool,
    pub authenticated: bool,
    pub consent_granted: bool,
    pub session_active: bool,
    pub session_id: Option<Uuid>,
    pub interview_id: Option<Uuid>,
    pub monitor_count: usize,
    pub violation_count: usize,
    pub uptime_seconds: u64,
}

impl IpcStatus {
    pub fn idle() -> Self {
        Self {
            connected: false,
            authenticated: false,
            consent_granted: false,
            session_active: false,
            session_id: None,
            interview_id: None,
            monitor_count: 0,
            violation_count: 0,
            uptime_seconds: 0,
        }
    }
}

pub struct IpcClient {
    stream: TcpStream,
}

impl IpcClient {
    pub async fn connect(port: u16) -> std::io::Result<Self> {
        let stream = TcpStream::connect(format!("127.0.0.1:{}", port)).await?;
        Ok(Self { stream })
    }

    pub async fn send(&mut self, request: &IpcRequest) -> std::io::Result<IpcResponse> {
        let mut json = serde_json::to_vec(request)?;
        json.push(b'\n');
        self.stream.write_all(&json).await?;
        self.stream.flush().await?;

        let mut reader = BufReader::new(&mut self.stream);
        let mut line = String::new();
        reader.read_line(&mut line).await?;
        serde_json::from_str(&line).map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e))
    }
}

pub struct IpcServer {
    listener: tokio::net::TcpListener,
    port: u16,
}

impl IpcServer {
    pub async fn bind() -> std::io::Result<Self> {
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await?;
        let port = listener.local_addr()?.port();
        Ok(Self { listener, port })
    }

    pub fn port(&self) -> u16 {
        self.port
    }

    pub fn write_port_file(&self) -> std::io::Result<()> {
        let data_dir = dirs::data_dir().unwrap_or_else(|| std::path::PathBuf::from("."));
        let ipc_dir = data_dir.join(".integrity-pro");
        std::fs::create_dir_all(&ipc_dir)?;
        let port_file = ipc_dir.join(".service-port");
        std::fs::write(&port_file, self.port.to_string())?;
        Ok(())
    }

    pub fn read_port_file() -> Option<u16> {
        let data_dir = dirs::data_dir().unwrap_or_else(|| std::path::PathBuf::from("."));
        let port_file = data_dir.join(SERVICE_PORT_FILE);
        std::fs::read_to_string(&port_file)
            .ok()
            .and_then(|s| s.trim().parse().ok())
    }

    pub async fn accept(&self) -> std::io::Result<IpcConnection> {
        let (stream, addr) = self.listener.accept().await?;
        Ok(IpcConnection { stream, addr })
    }
}

pub struct IpcConnection {
    pub stream: tokio::net::TcpStream,
    pub addr: std::net::SocketAddr,
}

impl IpcConnection {
    pub async fn read_request(&mut self) -> std::io::Result<IpcRequest> {
        let mut reader = BufReader::new(&mut self.stream);
        let mut line = String::new();
        reader.read_line(&mut line).await?;
        serde_json::from_str(&line).map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e))
    }

    pub async fn send_response(&mut self, response: &IpcResponse) -> std::io::Result<()> {
        let mut json = serde_json::to_vec(response)?;
        json.push(b'\n');
        self.stream.write_all(&json).await?;
        self.stream.flush().await
    }

    pub async fn send_telemetry(&mut self, event: &serde_json::Value) -> std::io::Result<()> {
        let response = IpcResponse::TelemetryEvent(event.clone());
        self.send_response(&response).await
    }
}
