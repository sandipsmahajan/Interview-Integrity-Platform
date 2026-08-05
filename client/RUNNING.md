# Integrity Pro Desktop Client

Pure Rust desktop application with two native binaries communicating via local TCP IPC.

## Architecture

```
integrity-service  (monitoring agent + IPC server)
       |
       |  JSON-line protocol over local TCP (random port)
       |  Port written to ~/.local/share/.integrity-pro/.service-port
       |
integrity-pro      (Slint UI, auto-spawns service, connects)
```

When run, `integrity-pro` automatically starts `integrity-service` in the background
if it is not already running. No manual coordination needed.

## Candidate Usage (Download & Run)

1. Download and unzip the release package for your platform
2. Run the launcher:
   - **Windows**: Double-click `launch.bat`
   - **Linux/macOS**: Run `./launch.sh`
3. The UI opens and handles everything automatically

The package contains just two binaries and a launcher script -- no installers, no admin rights required.

## Building a Release Package

From the workspace root:

```bash
cd client/scripts

# Build for current platform
./package-release.sh

# Cross-compile for Windows from Linux
./package-release.sh --target x86_64-pc-windows-gnu

# Cross-compile for macOS from Linux
./package-release.sh --target x86_64-apple-darwin
```

The zip is written to `client/dist/integrity-pro-<version>-<platform>.zip`.

## Prerequisites

- Rust toolchain (`rustup`: https://rustup.rs)
- Docker & Docker Compose (for backend infrastructure)

## Quick Start

### 1. Start Backend Services

From the workspace root:

```bash
./scripts/start-desktop-services.sh
```

This starts PostgreSQL, Redis, Kafka, MinIO, Mailpit, and 8 Spring Boot backend services. Wait for all services to report healthy (script waits automatically).

### 2. Build the Client

```bash
cd client
cargo build -p integrity-service -p integrity-pro
```

### 3. Run

Start the monitoring service first, then the UI:

```bash
# Terminal 1 — monitoring service
cargo run -p integrity-service

# Terminal 2 — Slint UI
cargo run -p integrity-pro
```

The service starts first and writes its port to `~/.local/share/.integrity-pro/.service-port`. The UI reads the port file on launch and connects.

## Development

### Build individually

```bash
cd client
cargo build -p integrity-service    # monitoring agent + IPC server
cargo build -p integrity-pro        # Slint UI
```

### Run with logging

```bash
RUST_LOG=integrity_pro=debug cargo run -p integrity-pro
RUST_LOG=integrity_service=debug cargo run -p integrity-service
```

### Run tests

```bash
cd client
cargo test --workspace
```

## Project Structure

```
client/
  integrity-pro/        # Slint UI binary
    src/main.rs         # App entry point, IPC client, screen callbacks
    ui/app.slint        # UI components: Welcome, Login, Consent, etc.
    build.rs            # slint_build::compile("ui/app.slint")
  integrity-service/    # Monitoring service binary
    src/main.rs         # Tokio runtime, ctrl-c handler
    src/server.rs       # IPC server, ServiceState, request handlers
  ipc/                  # IPC crate (shared protocol)
    src/lib.rs          # IpcServer, IpcClient, request/response types
  agent/                # Monitoring agent, collector registry
  policy/               # Policy engine (18 rules)
  system/               # System collectors (clipboard, audio, camera, etc.)
  config/               # Feature flags, collector configuration
  telemetry/            # Telemetry event types
  camera/               # Camera collector
  microphone/           # Microphone collector
  browser/              # Browser history collector
  network/              # Network connection collector
  storage/              # Local storage manager
  security/             # Signature verification
  logger/               # Audit logging
```

## IPC Protocol

The two binaries communicate over local TCP using a JSON-line protocol. Each message is a single JSON line terminated by `\n`.

### Port Discovery

- `integrity-service` binds to `127.0.0.1:0` (random port)
- Writes the port number to `~/.local/share/.integrity-pro/.service-port`
- `integrity-pro` reads the port file and connects on launch

### Request Types

| Request | Description |
|---------|-------------|
| `Ping` | Health check |
| `Authenticate` | Login with email/password |
| `GetInterview` | Fetch interview details |
| `AcceptConsent` | Accept monitoring consent |
| `DeclineConsent` | Decline consent |
| `StartInterview` | Begin interview session |
| `EndSession` | End session, get summary |
| `GetStatus` | Current service status |
| `GetAppInfo` | Device ID and version |
| `GetSystemChecks` | Pre-interview system checks |
| `GetRemoteConfig` | Fetch remote feature flags |
| `GetViolations` | Current violation count |
| `GetMetrics` | Session metrics |
| `LaunchContext` | Pass launch arguments |

## Feature Flags

Controlled via `client/config/src/lib.rs` and backend policy configuration:

```
enable_clipboard_monitoring
enable_overlay_detection
enable_fullscreen_detection
enable_idle_detection
enable_lock_screen_detection
enable_vpn_detection
enable_vm_detection
enable_screen_recording_detection
enable_remote_desktop_detection
enable_ai_assistant_detection
enable_browser_monitoring
enable_network_monitoring
enable_audio_monitoring
enable_camera_monitoring
enable_process_monitoring
enable_display_monitoring
```
