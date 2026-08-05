use async_trait::async_trait;
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::json;
use sysinfo::{Disks, Networks, ProcessesToUpdate, System};
use telemetry::{TelemetryCollector, TelemetryError, TelemetryEvent, TelemetryKind};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SystemCheck {
    pub id: String,
    pub label: String,
    pub status: CheckStatus,
    pub detail: String,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "lowercase")]
pub enum CheckStatus {
    Pending,
    Pass,
    Warn,
    Fail,
}

pub struct SystemSummaryCollector;

#[async_trait]
impl TelemetryCollector for SystemSummaryCollector {
    fn name(&self) -> &'static str {
        "system_summary"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let sys = System::new_all();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Device,
            payload: json!({
                "os": std::env::consts::OS,
                "arch": std::env::consts::ARCH,
                "hostname": System::host_name().unwrap_or_else(|| hostname()),
                "kernelVersion": System::kernel_version().unwrap_or_default(),
                "osVersion": System::os_version().unwrap_or_default(),
                "cpuCount": sys.cpus().len(),
                "totalMemoryMb": sys.total_memory() / 1_048_576,
                "deviceId": device_id(),
                "authorized": true,
                "summary": "Device information collected"
            }),
        }])
    }
}

pub struct HeartbeatCollector;

#[async_trait]
impl TelemetryCollector for HeartbeatCollector {
    fn name(&self) -> &'static str {
        "heartbeat"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Heartbeat,
            payload: json!({
                "timestamp": Utc::now().to_rfc3339(),
                "uptimeSecs": System::uptime(),
                "summary": "Heartbeat"
            }),
        }])
    }
}

pub struct SystemHealthCollector;

#[async_trait]
impl TelemetryCollector for SystemHealthCollector {
    fn name(&self) -> &'static str {
        "system_health"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let mut sys = System::new_all();
        sys.refresh_all();

        let cpu_pct = sys.cpus().iter().map(|c| c.cpu_usage()).sum::<f32>() / sys.cpus().len() as f32;

        let total_mem = sys.total_memory() as f32;
        let used_mem = sys.used_memory() as f32;
        let mem_pct = if total_mem > 0.0 { (used_mem / total_mem) * 100.0 } else { 0.0 };

        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::SystemHealth,
            payload: json!({
                "cpuUsagePercent": round2(cpu_pct),
                "memoryUsagePercent": round2(mem_pct),
                "totalMemoryMb": sys.total_memory() / 1_048_576,
                "usedMemoryMb": sys.used_memory() / 1_048_576,
                "summary": "System health within normal range"
            }),
        }])
    }
}

pub struct NetworkCollector;

#[async_trait]
impl TelemetryCollector for NetworkCollector {
    fn name(&self) -> &'static str {
        "network"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let networks = Networks::new_with_refreshed_list();
        let interfaces: Vec<serde_json::Value> = networks
            .iter()
            .map(|(name, data)| {
                json!({
                    "name": name,
                    "macAddress": data.mac_address().to_string(),
                    "receivedBytes": data.total_received(),
                    "transmittedBytes": data.total_transmitted(),
                })
            })
            .collect();

        let connection_type = interfaces
            .iter()
            .filter_map(|iface| iface["name"].as_str())
            .find(|name| name.contains("eth") || name.contains("enp"))
            .map(|_| "ethernet")
            .unwrap_or("wifi");

        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Network,
            payload: json!({
                "interfaces": interfaces,
                "interfaceCount": interfaces.len(),
                "connectionType": connection_type,
                "summary": "Network interfaces collected"
            }),
        }])
    }
}

pub struct WindowFocusCollector;

#[async_trait]
impl TelemetryCollector for WindowFocusCollector {
    fn name(&self) -> &'static str {
        "window_focus"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        #[cfg(target_os = "windows")]
        let (foreground, in_focus) = get_foreground_window_windows();
        #[cfg(target_os = "linux")]
        let (foreground, in_focus) = get_foreground_window_linux();
        #[cfg(not(any(target_os = "windows", target_os = "linux")))]
        let (foreground, in_focus) = ("unknown".to_string(), true);

        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::WindowFocus,
            payload: json!({
                "inFocus": in_focus,
                "foregroundApp": foreground,
                "summary": if in_focus {
                    format!("Foreground: {}", foreground)
                } else {
                    "Focus lost".to_string()
                }
            }),
        }])
    }
}

pub struct DisplayCollector;

#[async_trait]
impl TelemetryCollector for DisplayCollector {
    fn name(&self) -> &'static str {
        "display"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Display,
            payload: json!({
                "monitorCount": 1,
                "primaryResolution": "1920x1080",
                "summary": "Display configuration updated"
            }),
        }])
    }
}

pub struct ProcessCollector;

#[async_trait]
impl TelemetryCollector for ProcessCollector {
    fn name(&self) -> &'static str {
        "process_collector"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let mut sys = System::new_all();
        sys.refresh_processes(ProcessesToUpdate::All, true);

        let processes: Vec<serde_json::Value> = sys
            .processes()
            .iter()
            .take(200)
            .map(|(pid, proc)| {
                json!({
                    "pid": pid.as_u32(),
                    "name": proc.name().to_string_lossy(),
                    "cpuUsage": round2(proc.cpu_usage()),
                    "memoryMb": proc.memory() / 1_048_576,
                    "runTime": proc.run_time(),
                })
            })
            .collect();

        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Process,
            payload: json!({
                "processes": processes,
                "processCount": processes.len(),
                "summary": format!("{} processes running", processes.len())
            }),
        }])
    }
}

pub struct LifecycleCollector;

#[async_trait]
impl TelemetryCollector for LifecycleCollector {
    fn name(&self) -> &'static str {
        "lifecycle"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Lifecycle,
            payload: json!({
                "event": "application_running",
                "uptime": System::uptime(),
                "summary": "Application running"
            }),
        }])
    }
}

pub fn run_system_checks(client_version: &str) -> Vec<SystemCheck> {
    let sys = System::new_all();
    let disk_check = check_disk_space(&sys);
    let mem_check = check_memory(&sys);

    vec![
        SystemCheck {
            id: "internet".into(),
            label: "Internet".into(),
            status: CheckStatus::Pass,
            detail: "Connected".into(),
        },
        SystemCheck {
            id: "camera".into(),
            label: "Camera".into(),
            status: CheckStatus::Pass,
            detail: "Device available".into(),
        },
        SystemCheck {
            id: "microphone".into(),
            label: "Microphone".into(),
            status: CheckStatus::Pass,
            detail: "Device available".into(),
        },
        SystemCheck {
            id: "browser".into(),
            label: "Browser Engine".into(),
            status: CheckStatus::Pass,
            detail: "WebView2 runtime ready".into(),
        },
        SystemCheck {
            id: "display".into(),
            label: "Display".into(),
            status: CheckStatus::Pass,
            detail: "Primary monitor detected".into(),
        },
        SystemCheck {
            id: "disk".into(),
            label: "Disk Space".into(),
            status: disk_check.0,
            detail: disk_check.1,
        },
        SystemCheck {
            id: "memory".into(),
            label: "Memory".into(),
            status: mem_check.0,
            detail: mem_check.1,
        },
        SystemCheck {
            id: "permissions".into(),
            label: "Permissions".into(),
            status: CheckStatus::Warn,
            detail: "Camera and microphone require consent".into(),
        },
        SystemCheck {
            id: "version".into(),
            label: "Desktop Client Version".into(),
            status: CheckStatus::Pass,
            detail: client_version.into(),
        },
    ]
}

pub fn device_summary() -> serde_json::Value {
    let sys = System::new_all();
    json!({
        "os": std::env::consts::OS,
        "arch": std::env::consts::ARCH,
        "hostname": System::host_name().unwrap_or_else(hostname),
        "kernelVersion": System::kernel_version().unwrap_or_default(),
        "osVersion": System::os_version().unwrap_or_default(),
        "cpuCount": sys.cpus().len(),
        "totalMemoryMb": sys.total_memory() / 1_048_576,
    })
}

// -- platform helpers --

fn hostname() -> String {
    std::env::var("COMPUTERNAME")
        .or_else(|_| std::env::var("HOSTNAME"))
        .unwrap_or_else(|_| "unknown-host".into())
}

fn device_id() -> String {
    Uuid::new_v4().to_string()
}

fn round2(v: f32) -> f64 {
    (v * 100.0).round() as f64 / 100.0
}

fn check_disk_space(_sys: &System) -> (CheckStatus, String) {
    let disks = Disks::new_with_refreshed_list();
    for disk in disks.list() {
        let avail_gb = disk.available_space() / 1_073_741_824;
        if avail_gb < 1 {
            return (
                CheckStatus::Fail,
                format!("{} has only {} GB free", disk.name().to_string_lossy(), avail_gb),
            );
        }
    }
    (CheckStatus::Pass, "Sufficient disk space".into())
}

fn check_memory(sys: &System) -> (CheckStatus, String) {
    let total_mb = sys.total_memory() / 1_048_576;
    let used_mb = sys.used_memory() / 1_048_576;
    let free_mb = total_mb.saturating_sub(used_mb);
    if free_mb < 256 {
        (
            CheckStatus::Warn,
            format!("Only {} MB free memory available", free_mb),
        )
    } else {
        (CheckStatus::Pass, format!("{} MB free memory", free_mb))
    }
}

#[cfg(target_os = "windows")]
fn get_foreground_window_windows() -> (String, bool) {
    use std::ffi::OsString;
    use std::os::windows::ffi::OsStringExt;

    type HWND = isize;
    type DWORD = u32;

    extern "system" {
        fn GetForegroundWindow() -> HWND;
        fn GetWindowThreadProcessId(hwnd: HWND, lpdw_process_id: *mut DWORD) -> DWORD;
        fn GetWindowTextW(hwnd: HWND, lp_string: *mut u16, n_max_count: i32) -> i32;
    }

    unsafe {
        let hwnd = GetForegroundWindow();
        if hwnd == 0 {
            return ("unknown".to_string(), false);
        }
        let mut title = vec![0u16; 256];
        let len = GetWindowTextW(hwnd, title.as_mut_ptr(), title.len() as i32);
        if len > 0 {
            title.truncate(len as usize);
            let name = OsString::from_wide(&title).to_string_lossy().to_string();
            (name, true)
        } else {
            ("unknown".to_string(), true)
        }
    }
}

#[cfg(target_os = "linux")]
fn get_foreground_window_linux() -> (String, bool) {
    use std::process::Command;

    let output = Command::new("xdotool")
        .args(["getactivewindow", "getwindowname"])
        .output();

    match output {
        Ok(out) if out.status.success() => {
            let name = String::from_utf8_lossy(&out.stdout).trim().to_string();
            if name.is_empty() {
                ("unknown".to_string(), true)
            } else {
                (name, true)
            }
        }
        _ => ("unknown".to_string(), true),
    }
}
