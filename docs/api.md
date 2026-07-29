# API Contract

Base path: `/api/v1`

## Authentication

`POST /auth/login`

Request: `email`, `password`, `deviceId`

Response: `accessToken`, `refreshToken`, `expiresAt`

## Interview Management

`POST /interviews` creates an interview invitation with candidate, recruiter, meeting URL and start time.

`GET /interviews/recruiter/{recruiterId}` returns the recruiter's queue.

## Candidate Session

`POST /sessions` registers an authenticated desktop client and returns a session identifier.

## Telemetry

`POST /telemetry` ingests consented telemetry events. Payloads are schema-flexible JSON so OS-specific collectors can evolve independently.

Supported event types: `HEARTBEAT`, `DEVICE`, `DISPLAY`, `WINDOW_FOCUS`, `PROCESS`, `NETWORK`, `AUDIO`, `VIDEO`, `BROWSER`, `CRASH`.

## Reports

`GET /reports/sessions/{sessionId}` returns integrity score, violations, timeline-ready details and device summary.

## Notifications

`POST /notifications` queues recruiter, candidate, or admin notifications.

## WebSocket

`GET /ws/recruiter/telemetry` streams live recruiter telemetry notifications with backpressure protection.
