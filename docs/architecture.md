# Interview Integrity Platform Architecture

This repository is organized as an enterprise scaffold for a transparent, consent-driven interview integrity platform.

## Modules

- `backend`: Spring Boot 4.1 reactive service exposing authentication, interview management, session, telemetry, policy, notification and report APIs.
- `client`: Rust workspace for launcher, monitoring agent, embedded browser policy, IPC, storage, networking, update, telemetry and OS-specific collector abstractions.
- `portals`: React/Vite recruiter, candidate and admin portals.
- `infra`: Docker Compose, Kubernetes and ingress manifests.
- `docs`: API contract and Mermaid architecture diagrams.

## Privacy And Consent

The desktop client should collect telemetry only after explicit candidate authorization. Collectors are implemented behind Rust traits so each OS integration can enforce platform permissions, user disclosure, and least-privilege behavior.

## Clean Architecture Boundaries

Backend domain objects live under `domain`, command/query services under `application`, HTTP adapters under `api`, and database/security/messaging adapters under `infrastructure`. Application beans are registered explicitly through feature-scoped `@Configuration` classes.

## Event Flow

Telemetry enters through reactive WebFlux APIs, is persisted through R2DBC PostgreSQL, published through Reactor Kafka, evaluated by the policy engine, and exposed to dashboards over REST/WebSocket channels.
