# Interview Integrity Platform

Enterprise scaffold for transparent remote interview integrity workflows. The platform combines a Rust desktop client, secured embedded browser policy, consented telemetry, Spring Boot APIs, PostgreSQL persistence, Kafka events, Redis caching, React portals, and production deployment manifests.

## Repository Layout

- `backend/` - Java 21 Spring Boot 4.1 reactive backend with API, application, domain and infrastructure boundaries.
- `client/` - Rust workspace for launcher, agent, browser, policy, IPC, network, updater, storage, telemetry, security, screenshare, camera, microphone, system, logger and plugins.
- `portals/` - Recruiter, candidate and admin React/Vite applications.
- `infra/` - Docker Compose and Kubernetes manifests.
- `docs/` - Architecture, API notes and Mermaid diagrams.

## Local Development

Start platform dependencies:

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

Run backend checks:

```bash
gradle :backend:check
```

Write Gradle dependency locks after intentional dependency updates:

```bash
gradle dependencies --write-locks
```

Run Rust tests:

```bash
cd client
cargo test --workspace
```

Build portals:

```bash
cd portals
npm install
npm run build
```

## Integrity And Privacy Principles

The project is designed for user-authorized telemetry only. It does not attempt to bypass operating-system security, hide monitoring from candidates, or collect unrelated personal data. Platform-specific collectors must surface permissions clearly and report only the fields required by configured interview policies.

## Current Phase

This is the initial production scaffold covering phases 1-9 from architecture through testing/documentation foundations. OS-native telemetry, signed JWT issuance, WebSocket fanout, PDF rendering and cloud object storage adapters are represented by stable module boundaries ready for implementation.
