# Interview Integrity Platform

Enterprise scaffold for transparent remote interview integrity workflows. The platform combines a
Rust desktop client, secured embedded browser policy, consented telemetry, Spring Boot APIs,
PostgreSQL persistence, Kafka events, Redis caching, React portals, and production deployment
manifests.

## Repository Layout

- `libs/` - Shared Java 21 libraries (event contracts, security, common web components).
- `services/` - 19 Java 21 Spring Boot 4.1 reactive microservices (gateway, identity,
  organization, recruiter, candidate, interview, telemetry, policy, report, notification,
  analytics, audit, storage, feature-flag, scheduler, integration, configuration, desktop-client,
  discovery).
- `client/` - Rust workspace for launcher, agent, browser, policy, IPC, network, updater, storage,
  telemetry, security, screenshare, camera, microphone, system, logger and plugins.
- `portals/` - Recruiter and admin React/Vite applications.
- `infra/` - Docker Compose and Kubernetes manifests.
- `docs/` - Architecture, API notes and Mermaid diagrams.

## Local Development

Start platform dependencies:

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

Run all checks:

```bash
./gradlew check
```

Run a single service check (for example the identity service):

```bash
./gradlew :services:identity-service:check
```

For running the full stack locally in IntelliJ, see
`docs/local-development-intellij.md`.

Write Gradle dependency locks after intentional dependency updates:

```bash
gradle dependencies --write-locks
```

Run Rust tests:

```bash
cd client
cargo test --workspace
```

Run the WebView2 desktop app on Windows:

```bash
cd client
cargo run -p desktop-app
```

The `launcher` crate is a console smoke runner. The `desktop-app` crate is the Tauri 2 application
that opens a native desktop window backed by WebView2 on Windows. Use the MSVC Rust toolchain plus
Visual Studio C++ Build Tools for the smoothest Tauri/WebView2 setup:

```bash
rustup default stable-x86_64-pc-windows-msvc
```

Build portals:

```bash
cd portals
npm install
npm run build
```

## Integrity And Privacy Principles

The project is designed for user-authorized telemetry only. It does not attempt to bypass
operating-system security, hide monitoring from candidates, or collect unrelated personal data.
Platform-specific collectors must surface permissions clearly and report only the fields required by
configured interview policies.

## Current Phase

This is the initial production scaffold covering phases 1-9 from architecture through
testing/documentation foundations. OS-native telemetry, signed JWT issuance, WebSocket fanout, PDF
rendering and cloud object storage adapters are represented by stable module boundaries ready for
implementation.
