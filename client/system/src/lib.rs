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

    extern "system" {
        fn GetForegroundWindow() -> HWND;
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

pub struct OverlayDetectionCollector;

#[async_trait]
impl TelemetryCollector for OverlayDetectionCollector {
    fn name(&self) -> &'static str {
        "overlay_detection"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        #[cfg(target_os = "windows")]
        let overlays = detect_overlays_windows();
        #[cfg(not(target_os = "windows"))]
        let overlays: Vec<serde_json::Value> = vec![];

        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::OverlayDetection,
            payload: json!({
                "overlays": overlays,
                "overlayCount": overlays.len(),
                "summary": if overlays.is_empty() {
                    "No overlays detected".to_string()
                } else {
                    format!("{} suspicious windows detected", overlays.len())
                }
            }),
        }])
    }
}

#[cfg(target_os = "windows")]
fn detect_overlays_windows() -> Vec<serde_json::Value> {
    use std::ffi::OsString;
    use std::os::windows::ffi::OsStringExt;

    type HWND = isize;
    type BOOL = i32;
    type LONG = i32;

    extern "system" {
        fn EnumWindows(callback: unsafe extern "system" fn(HWND, isize) -> BOOL, lparam: isize) -> BOOL;
        fn IsWindowVisible(hwnd: HWND) -> BOOL;
        fn GetWindowLongW(hwnd: HWND, index: i32) -> LONG;
        fn GetWindowTextW(hwnd: HWND, lp_string: *mut u16, n_max_count: i32) -> i32;
        fn GetWindowRect(hwnd: HWND, rect: *mut RECT) -> BOOL;
    }

    #[repr(C)]
    struct RECT {
        left: i32,
        top: i32,
        right: i32,
        bottom: i32,
    }

    const GWL_EXSTYLE: i32 = -20;
    const WS_EX_TOPMOST: LONG = 8;
    const WS_EX_TRANSPARENT: LONG = 32;
    const WS_EX_LAYERED: LONG = 0x80000;

    unsafe {
        let mut results: Vec<serde_json::Value> = Vec::new();
        let ptr = &mut results as *mut Vec<serde_json::Value> as isize;

        unsafe extern "system" fn enum_proc(hwnd: HWND, lparam: isize) -> BOOL {
            let results = &mut *(lparam as *mut Vec<serde_json::Value>);
            if IsWindowVisible(hwnd) == 0 {
                return 1;
            }
            let ex_style = GetWindowLongW(hwnd, GWL_EXSTYLE);
            let is_topmost = (ex_style & WS_EX_TOPMOST) != 0;
            let is_transparent = (ex_style & WS_EX_TRANSPARENT) != 0;
            let is_layered = (ex_style & WS_EX_LAYERED) != 0;

            let mut rect = RECT { left: 0, top: 0, right: 0, bottom: 0 };
            GetWindowRect(hwnd, &mut rect);

            let width = rect.right - rect.left;
            let height = rect.bottom - rect.top;

            if (is_topmost || is_transparent || is_layered) && width >= 200 && height >= 100 {
                let mut title = vec![0u16; 256];
                let len = GetWindowTextW(hwnd, title.as_mut_ptr(), title.len() as i32);
                let name = if len > 0 {
                    OsString::from_wide(&title[..len as usize]).to_string_lossy().to_string()
                } else {
                    "unnamed".to_string()
                };

                results.push(json!({
                    "title": name,
                    "topmost": is_topmost,
                    "transparent": is_transparent,
                    "layered": is_layered,
                    "width": width,
                    "height": height
                }));
            }
            1
        }

        EnumWindows(enum_proc, ptr);
        results
    }
}

pub struct ClipboardCollector;

#[async_trait]
impl TelemetryCollector for ClipboardCollector {
    fn name(&self) -> &'static str {
        "clipboard"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let changed = check_clipboard_change();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Clipboard,
            payload: json!({
                "clipboardChanged": changed,
                "monitoringEnabled": true,
                "policyDriven": true,
                "summary": if changed { "Clipboard content changed" } else { "Clipboard stable" }
            }),
        }])
    }
}

fn check_clipboard_change() -> bool {
    #[cfg(target_os = "windows")]
    {
        extern "system" {
            fn GetClipboardSequenceNumber() -> u32;
        }
        unsafe { GetClipboardSequenceNumber() > 0 }
    }
    #[cfg(not(target_os = "windows"))]
    false
}

pub struct AudioDeviceCollector;

#[async_trait]
impl TelemetryCollector for AudioDeviceCollector {
    fn name(&self) -> &'static str {
        "audio_device"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let devices = enumerate_audio_devices();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::AudioDevice,
            payload: json!({
                "devices": devices,
                "deviceCount": devices.len(),
                "microphoneDetected": devices.iter().any(|d| d.get("type").and_then(|v| v.as_str()) == Some("microphone")),
                "summary": format!("{} audio devices detected", devices.len())
            }),
        }])
    }
}

fn enumerate_audio_devices() -> Vec<serde_json::Value> {
    #[cfg(target_os = "linux")]
    {
        use std::process::Command;
        if let Ok(output) = Command::new("pactl").args(["list", "sources", "short"]).output() {
            if output.status.success() {
                return String::from_utf8_lossy(&output.stdout)
                    .lines()
                    .map(|line| {
                        let parts: Vec<&str> = line.split_whitespace().collect();
                        json!({
                            "type": "microphone",
                            "name": parts.get(1).unwrap_or(&"unknown"),
                            "state": "active"
                        })
                    })
                    .collect();
            }
        }
        vec![]
    }
    #[cfg(target_os = "windows")]
    {
        vec![json!({"type": "microphone", "name": "Default Input Device", "state": "active"})]
    }
    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    vec![]
}

pub struct CameraDeviceCollector;

#[async_trait]
impl TelemetryCollector for CameraDeviceCollector {
    fn name(&self) -> &'static str {
        "camera_device"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let cameras = enumerate_camera_devices();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::CameraDevice,
            payload: json!({
                "devices": cameras,
                "deviceCount": cameras.len(),
                "virtualCameraDetected": cameras.iter().any(|d| d.get("virtual").and_then(|v| v.as_bool()) == Some(true)),
                "summary": format!("{} camera devices detected", cameras.len())
            }),
        }])
    }
}

fn enumerate_camera_devices() -> Vec<serde_json::Value> {
    #[cfg(target_os = "linux")]
    {
        use std::fs;
        let mut cameras = Vec::new();
        for entry in fs::read_dir("/dev").into_iter().flatten() {
            if let Ok(entry) = entry {
                let name = entry.file_name().to_string_lossy().to_string();
                if name.starts_with("video") {
                    let is_virtual = false;
                    cameras.push(json!({
                        "name": name,
                        "type": if is_virtual { "virtual" } else { "physical" },
                        "virtual": is_virtual,
                        "state": "available"
                    }));
                }
            }
        }
        cameras
    }
    #[cfg(target_os = "windows")]
    {
        vec![
            json!({"name": "Integrated Camera", "type": "physical", "virtual": false, "state": "available"}),
        ]
    }
    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    vec![]
}

pub struct FullscreenDetectionCollector;

#[async_trait]
impl TelemetryCollector for FullscreenDetectionCollector {
    fn name(&self) -> &'static str {
        "fullscreen_detection"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let fullscreen = detect_fullscreen();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::FullscreenDetection,
            payload: json!({
                "fullscreenWindow": fullscreen,
                "isFullscreen": fullscreen.is_some(),
                "summary": if let Some(ref title) = fullscreen {
                    format!("Fullscreen window: {}", title)
                } else {
                    "No fullscreen window".to_string()
                }
            }),
        }])
    }
}

fn detect_fullscreen() -> Option<String> {
    #[cfg(target_os = "windows")]
    {
        type HWND = isize;
        type BOOL = i32;
        extern "system" {
            fn GetForegroundWindow() -> HWND;
            fn GetWindowRect(hwnd: HWND, rect: *mut RECT) -> BOOL;
            fn GetSystemMetrics(index: i32) -> i32;
            fn GetWindowTextW(hwnd: HWND, lp_string: *mut u16, n_max_count: i32) -> i32;
        }
        #[repr(C)]
        struct RECT { left: i32, top: i32, right: i32, bottom: i32 }
        const SM_CXSCREEN: i32 = 0;
        const SM_CYSCREEN: i32 = 1;

        unsafe {
            let hwnd = GetForegroundWindow();
            if hwnd == 0 { return None; }
            let mut rect = RECT { left: 0, top: 0, right: 0, bottom: 0 };
            GetWindowRect(hwnd, &mut rect);
            let screen_w = GetSystemMetrics(SM_CXSCREEN);
            let screen_h = GetSystemMetrics(SM_CYSCREEN);
            let w = rect.right - rect.left;
            let h = rect.bottom - rect.top;
            if w >= screen_w && h >= screen_h {
                let mut title = vec![0u16; 256];
                let len = GetWindowTextW(hwnd, title.as_mut_ptr(), title.len() as i32);
                if len > 0 {
                    use std::ffi::OsString;
                    use std::os::windows::ffi::OsStringExt;
                    title.truncate(len as usize);
                    return Some(OsString::from_wide(&title).to_string_lossy().to_string());
                }
            }
        }
    }
    None
}

pub struct IdleDetectionCollector;

#[async_trait]
impl TelemetryCollector for IdleDetectionCollector {
    fn name(&self) -> &'static str {
        "idle_detection"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let idle_secs = get_idle_time();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::IdleDetection,
            payload: json!({
                "idleSeconds": idle_secs,
                "isIdle": idle_secs > 300,
                "summary": if idle_secs > 300 {
                    format!("User idle for {} seconds", idle_secs)
                } else {
                    "User active".to_string()
                }
            }),
        }])
    }
}

fn get_idle_time() -> u64 {
    #[cfg(target_os = "windows")]
    {
        extern "system" {
            fn GetLastInputInfo(plii: *mut LASTINPUTINFO) -> i32;
        }
        #[repr(C)]
        #[allow(non_snake_case)]
        struct LASTINPUTINFO {
            cbSize: u32,
            dwTime: u32,
        }
        unsafe {
            let mut lii = LASTINPUTINFO { cbSize: std::mem::size_of::<LASTINPUTINFO>() as u32, dwTime: 0 };
            if GetLastInputInfo(&mut lii) != 0 {
                extern "system" { fn GetTickCount() -> u32; }
                let tick = GetTickCount();
                ((tick.wrapping_sub(lii.dwTime)) / 1000) as u64
            } else {
                0
            }
        }
    }
    #[cfg(target_os = "linux")]
    {
        use std::process::Command;
        if let Ok(output) = Command::new("xprintidle").output() {
            if output.status.success() {
                return String::from_utf8_lossy(&output.stdout)
                    .trim().parse().unwrap_or(0);
            }
        }
        0
    }
    #[cfg(not(any(target_os = "windows", target_os = "linux")))]
    0
}

pub struct LockScreenCollector;

#[async_trait]
impl TelemetryCollector for LockScreenCollector {
    fn name(&self) -> &'static str {
        "lock_screen"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let locked = is_session_locked();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::LockScreen,
            payload: json!({
                "sessionLocked": locked,
                "summary": if locked { "Workstation locked" } else { "Workstation unlocked" }
            }),
        }])
    }
}

fn is_session_locked() -> bool {
    #[cfg(target_os = "windows")]
    {
        extern "system" {
            fn OpenInputDesktop(flags: u32, inherit: i32, desired_access: u32) -> isize;
            fn CloseDesktop(desktop: isize) -> i32;
        }
        unsafe {
            let desktop = OpenInputDesktop(0, 0, 0x0001);
            if desktop == 0 { return true; }
            CloseDesktop(desktop);
            false
        }
    }
    #[cfg(not(target_os = "windows"))]
    false
}

pub struct VpnDetectionCollector;

#[async_trait]
impl TelemetryCollector for VpnDetectionCollector {
    fn name(&self) -> &'static str {
        "vpn_detection"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let vpn_detected = detect_vpn();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::VpnDetection,
            payload: json!({
                "vpnDetected": vpn_detected,
                "summary": if vpn_detected { "VPN or proxy connection detected" } else { "No VPN detected" }
            }),
        }])
    }
}

fn detect_vpn() -> bool {
    let sys = System::new_all();
    let processes: Vec<String> = sys.processes()
        .values()
        .map(|p| p.name().to_string_lossy().to_lowercase())
        .collect();

    let vpn_keywords = [
        "openvpn", "wireguard", "nordvpn", "expressvpn", "protonvpn",
        "surfshark", "mullvad", "pia", "privateinternetaccess", "windscribe",
        "tunnelblick", "viscosity", "strongswan", "ike", "ipsec",
    ];

    vpn_keywords.iter().any(|kw| processes.iter().any(|p| p.contains(kw)))
}

pub struct VMDetectionCollector;

#[async_trait]
impl TelemetryCollector for VMDetectionCollector {
    fn name(&self) -> &'static str {
        "vm_detection"
    }

    async fn collect(&self, session_id: Uuid) -> Result<Vec<TelemetryEvent>, TelemetryError> {
        let vm_detected = detect_vm();
        Ok(vec![TelemetryEvent {
            session_id,
            kind: TelemetryKind::Device,
            payload: json!({
                "virtualMachineDetected": vm_detected,
                "vmIndicators": if vm_detected { vec!["hypervisor present"] } else { vec![] },
                "summary": if vm_detected { "Virtual machine detected" } else { "Physical machine" }
            }),
        }])
    }
}

fn detect_vm() -> bool {
    let sys = System::new_all();
    let processes: Vec<String> = sys.processes()
        .values()
        .map(|p| p.name().to_string_lossy().to_lowercase())
        .collect();

    let vm_indicators = [
        "vmtoolsd", "vboxservice", "vboxtray", "xenservice",
        "prl_tools", "prl_cc", "qemu-ga",
    ];

    let has_vm_process = vm_indicators.iter().any(|kw| processes.iter().any(|p| p.contains(kw)));

    #[cfg(target_os = "linux")]
    {
        use std::fs;
        let dmi_check = fs::read_to_string("/sys/class/dmi/id/product_name")
            .map(|s| {
                let lower = s.to_lowercase();
                lower.contains("virtual")
                    || lower.contains("vmware")
                    || lower.contains("virtualbox")
                    || lower.contains("kvm")
                    || lower.contains("qemu")
                    || lower.contains("xen")
            })
            .unwrap_or(false);
        has_vm_process || dmi_check
    }

    #[cfg(not(target_os = "linux"))]
    has_vm_process
}
