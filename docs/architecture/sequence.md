# Sequence Diagrams

**Purpose.** To show how the platform behaves at runtime for the most important flows. These
diagrams are also the reference for load-testing, debugging, and on-call triage.

## 1. Login / authentication flow

```mermaid
sequenceDiagram
    participant U as User (browser/client)
    participant GW as api-gateway
    participant ID as identity-service
    participant RD as Redis
    participant DB as identity DB

    U->>GW: POST /api/v1/auth/login {username, password}
    GW->>ID: forward login request
    ID->>ID: validate credentials (bcrypt/argon)
    alt success
        ID->>DB: verify user + role
        ID->>ID: sign JWT access token (short TTL)
        ID->>ID: sign refresh token (long TTL)
        ID->>RD: store refresh-token hash (keyed by token id)
        ID-->>GW: access + refresh tokens
        GW-->>U: 200 {accessToken, refreshToken, expiresIn}
    else failure
        ID-->>GW: 401 Unauthorized
        GW-->>U: 401
    end
```

**Notes**
- The gateway is the only component that ever sees credentials.
- Access tokens are short-lived and validated in `libs/security` on every request (stateless).
- Refresh tokens are validated against Redis, which lets the platform revoke sessions instantly.

## 2. Candidate onboarding (consent flow)

```mermaid
sequenceDiagram
    participant R as Recruiter
    participant GW as api-gateway
    participant CS as candidate-service
    participant OS as organization-service
    participant KF as Kafka
    participant AU as audit-service
    participant NT as notification-service

    R->>GW: POST /api/v1/candidates
    GW->>CS: create candidate
    CS->>CS: validate org membership via OS
    CS->>CS: persist candidate + consent record (state = PENDING)
    CS->>KF: publish candidate.created event
    KF->>AU: audit.candidate.created
    KF->>NT: notification.send consent-email
    CS-->>GW: 201 candidate summary
    GW-->>R: 201
```

**Notes**
- The candidate record is only considered active once consent is recorded (`candidate.consent`).
- Downstream actions (audit, notification) happen via Kafka, so they never block the API response.

## 3. Interview lifecycle

```mermaid
sequenceDiagram
    participant R as Recruiter
    participant GW as api-gateway
    participant IV as interview-service
    participant DC as desktop-client-service
    participant CL as Rust desktop client
    participant KF as Kafka
    participant PE as policy-engine-service
    participant RP as report-service

    R->>GW: POST /api/v1/interviews {candidateId, schedule}
    GW->>IV: create interview
    IV->>IV: persist interview (state = SCHEDULED)
    IV->>KF: publish interview.scheduled
    IV-->>GW: 201 interview payload
    GW-->>R: 201

    CL->>GW: POST /api/v1/desktop-client/pair {interviewId, code}
    GW->>DC: pair request
    DC->>DC: verify pairing code
    DC-->>CL: paired + session token

    CL->>GW: telemetry stream (heartbeats, screen/camera/audio events)
    GW->>TL: forward telemetry
    TL->>KF: publish telemetry.received (topic per interview)
    KF->>PE: consume telemetry.received
    PE->>PE: evaluate policy rules
    PE->>KF: publish policy.violation / policy.approved

    KF->>RP: consume policy results + telemetry
    RP->>RP: assemble integrity report
    RP-->>GW: report available (when interview ends)
    GW-->>R: report status
```

**Notes**
- Telemetry is **fire-and-forget** to Kafka; the client never blocks on policy evaluation.
- The policy engine is a pure event consumer, so it can be scaled independently.
- The report service produces the human-readable integrity report at interview end.

## 4. Secrets rotation

```mermaid
sequenceDiagram
    participant SM as Secrets Manager
    participant L as Lambda (rotation)
    participant P as Pods (services)
    participant DB as RDS / MSK

    SM->>L: rotation event (every 90 days)
    L->>SM: create new secret version
    L->>DB: update credential (ALTER ROLE / scram secret)
    DB-->>L: ok
    L->>SM: mark new version as current
    SM-->>L: rotated
    P->>SM: fetch current version on pod start
    P->>DB: connect with new credential
```

**Notes**
- Rotation applies to prod (and is available for uat); the JWT signing key is rotated by issuing a
  new KMS/SM version and updating the `integrity-secrets` Secret, followed by a rolling restart.
- Runbooks: `runbooks/secret-rotation.md`, `runbooks/password-rotation.md`.

## 5. How to read these diagrams when debugging

| Symptom | Which sequence to look at | First thing to check |
|---|---|---|
| User cannot log in | §1 | Is `identity-service` healthy? Is Redis reachable (refresh tokens)? |
| Candidate email never arrives | §2 | Is the `candidate.created` event on Kafka? Is `notification-service` consuming? |
| No integrity report at interview end | §3 | Is telemetry reaching Kafka? Is `policy-engine-service` consuming? |
| Services can't authenticate to DB after rotation | §4 | Did the rotation Lambda update the DB before marking the version current? |
