# API Contract

All service APIs are aggregated behind the API gateway. Every path below is relative to the
gateway host (port `8080` in local development).

Base path: `/api/v1`

Authentication: every endpoint except OpenAPI/actuator health requires a `Authorization: Bearer
<accessToken>` header issued by `POST /auth/login` (or `/auth/mfa/verify`). The token carries the
organization id and user id; services re-check membership in business logic in addition to
database row-level security.

Each service also exposes springdoc OpenAPI at `http://localhost:<port>/v3/api-docs` and the
Swagger UI at `http://localhost:<port>/swagger-ui.html` on its own port. The gateway exposes the
public routes listed below.

## Authentication (identity-service)

- `POST /auth/register` - register an organization and its first admin; returns tokens
- `POST /auth/login` - password login; returns tokens, may return an MFA challenge
- `POST /auth/mfa/verify` - complete an MFA challenge (TOTP or recovery code) and receive tokens
- `POST /auth/mfa/email-otp` - request an email one-time password for a challenge
- `POST /auth/mfa/totp/enroll` - start TOTP enrollment
- `POST /auth/mfa/totp/verify` - finish TOTP enrollment
- `POST /auth/mfa/recovery-codes/regenerate` - rotate recovery codes
- `GET /auth/mfa/devices` / `GET /auth/mfa/trusted-devices` - list MFA devices / trusted devices
- `DELETE /auth/mfa/devices/{deviceId}` / `DELETE /auth/mfa/trusted-devices/{id}` - remove a device
- `POST /auth/otp/send` - send an email OTP
- `POST /auth/otp/verify` - verify an email OTP
- `POST /auth/refresh` - exchange a refresh token for a new access token
- `POST /auth/logout` - revoke the session associated with a refresh token
- `POST /auth/verify-email` - verify an email address with a token
- `POST /auth/password/reset-request` - request a password reset
- `POST /auth/password/reset` - perform the password reset
- `GET /auth/sessions` / `DELETE /auth/sessions/{sessionId}` / `DELETE /auth/sessions` - manage the
  caller's own sessions

## Identity / IAM

- `GET /users`, `POST /users` - list users (paged) and create a user (regular users are created via
  organization registration)
- `GET /users/{userId}`, `PATCH /users/{userId}`, `DELETE /users/{userId}`
- `PATCH /users/{userId}/status` - activate/deactivate a user
- `POST /users/{userId}/roles` - assign roles
- `GET /roles`, `POST /roles`; `GET /roles/{roleId}`, `PATCH /roles/{roleId}`,
  `DELETE /roles/{roleId}`
- `POST /roles/{roleId}/permissions`, `DELETE /roles/{roleId}/permissions/{permissionId}`
- `GET /permissions`

## Organization

- `GET /organizations`, `PATCH /organizations` - current tenant organization
- `GET /organizations/departments`, `POST /organizations/departments`; `GET /departments/{id}`,
  `PATCH /departments/{id}`, `DELETE /departments/{id}`
- `GET /organizations/teams`, `POST /organizations/teams`; `GET /teams/{teamId}`,
  `PATCH /teams/{teamId}`, `DELETE /teams/{teamId}`
- `GET /teams/{teamId}/members`, `POST /teams/{teamId}/members`,
  `DELETE /teams/{teamId}/members/{userId}`
- `GET /departments/{departmentId}/teams` - teams in a department
- `GET /plans/{code}` - plan details
- `POST /organizations/status` - update tenant status
- `POST /organizations/subscription/cancel` / `POST /organizations/subscription/resume`
- `GET /organizations/domains`, `POST /organizations/domains/{domainId}/verify`,
  `DELETE /organizations/domains/{domainId}` - verified domains

## Recruiters

- `GET /recruiters`, `POST /recruiters` - list/create recruiters
- `GET /recruiters/me` - the caller's own profile
- `GET /recruiters/{recruiterId}`, `PATCH /recruiters/{recruiterId}`,
  `DELETE /recruiters/{recruiterId}`
- `POST /recruiters/{recruiterId}/status` - activate/deactivate
- `GET /recruiters/{recruiterId}/profile`, `PATCH /recruiters/{recruiterId}/profile`
- `GET /stages`, `POST /stages`; `GET /stages/{stageId}`, `PATCH /stages/{stageId}`,
  `DELETE /stages/{stageId}`; `GET /stages/{stageId}/candidates`
- `GET /candidates/{candidateId}/assignments`, `POST /candidates/{candidateId}/assignments`;
  `PATCH /assignments/{assignmentId}/role`, `POST /assignments/{assignmentId}/end`
- `GET /candidates/{candidateId}/notes`, `POST /candidates/{candidateId}/notes`;
  `PATCH /notes/{noteId}`, `DELETE /notes/{noteId}`
- `POST /candidates/{candidateId}/pipeline/exit` - move a candidate to the exited stage

## Candidates

- `GET /candidates`, `POST /candidates` - list/create candidates
- `GET /candidates/{candidateId}`, `PATCH /candidates/{candidateId}`,
  `DELETE /candidates/{candidateId}`
- `POST /candidates/{candidateId}/status` - status transition
- `GET /candidates/{candidateId}/profile`, `PATCH /candidates/{candidateId}/profile`
- `GET /candidates/{candidateId}/assessments`; `POST /candidates/{candidateId}/assessments/{id}/start`,
  `/complete`, `/expire`
- `GET /candidates/{candidateId}/consents`; `POST /candidates/{candidateId}/consents/{consentId}/revoke`
- `GET /candidates/{candidateId}/documents`, `POST /candidates/{candidateId}/documents`;
  `DELETE /candidates/{candidateId}/documents/{documentId}`
- `GET /candidates/{candidateId}/notes`, `POST /candidates/{candidateId}/notes`;
  `PATCH /candidates/{candidateId}/notes/{noteId}`,
  `PATCH /candidates/{candidateId}/notes/{noteId}/pin`, `DELETE /candidates/{candidateId}/notes/{noteId}`
- `GET /candidates/{candidateId}/tags`, `POST /candidates/{candidateId}/tags`,
  `DELETE /candidates/{candidateId}/tags/{tagId}`
- `GET /tags`, `POST /tags`, `DELETE /tags/{tagId}`

## Interviews

- `GET /interviews`, `POST /interviews` - list/create interviews
- `GET /interviews/{interviewId}`, `PATCH /interviews/{interviewId}`,
  `DELETE /interviews/{interviewId}`
- `POST /interviews/{interviewId}/schedule` - schedule with start/end/timezone/meeting url
- `POST /interviews/{interviewId}/cancel` / `POST /interviews/{interviewId}/no-show`
- `POST /interviews/{interviewId}/sessions` - create an interview session;
  `GET /interviews/{interviewId}/sessions` - list sessions
- `POST /sessions/{sessionId}/pause`, `/resume`, `/complete`, `/abnormal` - lifecycle transitions
  (routed by the gateway to interview-service)
- `GET /interviews/{interviewId}/panel`, `POST /interviews/{interviewId}/panel`;
  `PATCH /interviews/{interviewId}/panel/{interviewerId}`, `DELETE .../panel/{interviewerId}`
- `GET /interviews/{interviewId}/feedback`, `POST /interviews/{interviewId}/feedback`;
  `PATCH /interviews/{interviewId}/feedback/{feedbackId}`,
  `POST /interviews/{interviewId}/feedback/{feedbackId}/submit`,
  `DELETE /interviews/{interviewId}/feedback/{feedbackId}`
- `GET /interviews/{interviewId}/calendar-events`; `PATCH /interviews/{interviewId}/calendar-events/{eventId}`
- `GET /interviewers`, `POST /interviewers`; `GET /interviewers/{interviewerId}`,
  `PATCH /interviewers/{interviewerId}`, `DELETE /interviewers/{interviewerId}`

## Telemetry (desktop client ingestion)

- `POST /sessions/{sessionId}/events` - ingest consented telemetry events
- `GET /sessions/{sessionId}/events/count` - event count for a session
- `GET /sessions/{sessionId}` - session detail
- `GET /sessions/{sessionId}/summary` - session summary
- `POST /sessions/{sessionId}/status` - update session status
- `GET /event-types`, `POST /event-types` - supported telemetry event types

Supported event types: `HEARTBEAT`, `DEVICE`, `DISPLAY`, `WINDOW_FOCUS`, `PROCESS`, `NETWORK`,
`AUDIO`, `VIDEO`, `BROWSER`, `CRASH`.

## Policies and Violations

- `GET /policies`, `POST /policies`; `GET /policies/{policyId}`, `PUT /policies/{policyId}`,
  `DELETE /policies/{policyId}`; `POST /policies/{policyId}/status`
- `GET /policies/{policyId}/rules`, `POST /policies/{policyId}/rules`;
  `PUT /policies/{policyId}/rules/{ruleId}`, `DELETE /policies/{policyId}/rules/{ruleId}`
- `POST /policies/{policyId}/evaluate` - evaluate a payload against the policy
- `POST /policies/{policyId}/violate` - record a manual violation
- `GET /violations`, `POST /violations`; `GET /violations/{violationId}`
- `POST /violations/{violationId}/review` - review with action/comment/escalation

## Reports

- `GET /reports`, `POST /reports`; `GET /reports/{reportId}`
- `POST /reports/{reportId}/generate` / `/regenerate` / `/expire`
- `GET /reports/{reportId}/sections`, `POST /reports/{reportId}/sections`;
  `GET /reports/{reportId}/sections/{sectionId}`, `PATCH .../sections/{sectionId}`,
  `DELETE .../sections/{sectionId}`
- `GET /reports/{reportId}/requests`; `GET /reports/{reportId}/requests/{requestId}`,
  `POST /reports/{reportId}/requests/{requestId}/complete`, `/fail`
- `GET /report-schedules`, `POST /report-schedules`; `GET /report-schedules/{scheduleId}`,
  `PATCH /report-schedules/{scheduleId}`, `DELETE /report-schedules/{scheduleId}`;
  `POST /report-schedules/{scheduleId}/enable` / `/disable`

## Notifications

- `GET /notifications` - the caller's own notifications (filtered by status); requires a user id
- `GET /notifications/{notificationId}` - a single notification owned by the caller
- `POST /notifications/{notificationId}/read` - mark the caller's notification as read
- `GET /notifications/{notificationId}/deliveries` - delivery attempts for the caller's notification
- `POST /notifications/{notificationId}/sent` / `/delivered` / `/failed` - provider outcome callbacks
  (same organization; used by the email provider)
- `GET /notification-templates`, `POST /notification-templates`; `GET /notification-templates/{id}`,
  `PUT /notification-templates/{id}`, `DELETE /notification-templates/{id}`;
  `POST /notification-templates/{id}/default`
- `GET /notification-preferences/users/{userId}`, `PUT /notification-preferences/users/{userId}`,
  `POST /notification-preferences/users/{userId}` (bulk)
- `GET /notification-preferences/users/{userId}/list`

## Analytics

- `GET /analytics/organization`, `/recruiter`, `/candidate`, `/interview`, `/integrity` - summary
  rows; each also accepts `/range` for a date-range query
- `POST /analytics/organization`, `/recruiter`, `/candidate`, `/interview`, `/integrity` - create a summary
- `POST /analytics/refresh-monthly` - trigger the monthly rollup job
- `GET /analytics/job-runs`; `POST /analytics/job-runs/{jobRunId}/succeed` / `/fail`

## Audit

- `GET /audit-events` - paged audit trail (resource type, outcome, date filters)
- `GET /audit-events/{eventId}` - a single audit event
- `GET /audit-events/{eventId}/changes` - field-level changes for an event
- `GET /api-audit-log` - API-level audit log (gateway-facing audit entries)

## Storage

- `GET /buckets`, `POST /buckets`; `GET /buckets/{bucketId}`, `PATCH /buckets/{bucketId}`,
  `DELETE /buckets/{bucketId}`
- `POST /buckets/{bucketId}/objects` - upload an object to a bucket
- `GET /objects`, `GET /objects/{objectId}`, `PATCH /objects/{objectId}`,
  `DELETE /objects/{objectId}`
- `GET /objects/{objectId}/versions`, `GET /objects/{objectId}/history`
- `GET /objects/{objectId}/signed-urls`, `POST /objects/{objectId}/signed-urls`; `GET /signed-urls/{urlId}`,
  `POST /signed-urls/{urlId}/revoke`

## Feature Flags

- `GET /features`, `POST /features`; `GET /features/{featureId}`, `PATCH /features/{featureId}`,
  `DELETE /features/{featureId}`
- `GET /features/{featureId}/flags`, `POST /features/{featureId}/flags`; `GET /flags/{flagId}`,
  `PATCH /flags/{flagId}`; `GET /flags/{flagId}/history`
- `GET /flags/{flagId}/targets`, `POST /flags/{flagId}/targets`,
  `DELETE /flags/{flagId}/targets/{userId}`
- `GET /experiments`, `POST /experiments`; `GET /experiments/{experimentId}`,
  `PATCH /experiments/{experimentId}`; `POST /experiments/{experimentId}/start`, `/pause`, `/resume`,
  `/complete`, `/reject`

## Scheduler

- `GET /scheduled-jobs`, `PUT /scheduled-jobs/{jobId}`; `GET /scheduled-jobs/{jobId}`,
  `DELETE /scheduled-jobs/{jobId}`
- `POST /scheduled-jobs/{jobId}/enable` / `/disable` / `/pause` / `/resume`
- `GET /scheduled-jobs/due`; `POST /scheduled-jobs/run-due`
- `GET /job-executions`, `POST /job-executions/{executionId}/complete`, `/fail`, `/skip`, `/timeout`
- `POST /job-locks/{jobId}/acquire` / `POST /job-locks/{jobId}/release`

## Integrations

- `GET /integrations`, `PUT /integrations/{integrationId}`; `GET /integrations/{integrationId}`,
  `DELETE /integrations/{integrationId}`; `POST /integrations/{integrationId}/connect` / `/disconnect`;
  `PATCH /integrations/{integrationId}/error`
- `GET /integration-connections/{connectionId}`, `POST /integration-connections/{connectionId}/connect`,
  `/disconnect`, `/sync`, `/error`
- `GET /integration-webhooks`, `PUT /integration-webhooks/{webhookId}`;
  `GET /integration-webhooks/{webhookId}`, `POST /integration-webhooks/{webhookId}/enable` / `/disable`
- `GET /integration-sync-logs`; `POST /integration-sync-logs/{syncLogId}/complete` / `/fail`

## Configuration

- `GET /configurations`, `POST /configurations`; `GET /configurations/{configurationId}`,
  `PATCH /configurations/{configurationId}`, `DELETE /configurations/{configurationId}`
- `GET /configurations/{configurationId}/history` - version history
- `GET /configuration-schema`, `POST /configuration-schema`; `GET /configuration-schema/{schemaId}`,
  `PATCH /configuration-schema/{schemaId}`, `DELETE /configuration-schema/{schemaId}`

## WebSocket

The desktop client connects to the desktop-client-service WebSocket relay:

`GET /ws/desktop` (port `8086`, not through the API gateway)

It bridges the desktop agent to the platform topics over Kafka. Malformed or unauthorized frames are
rejected; the relay applies backpressure to protect the client connection.

## Gateway Notes

- Routes use the `lb://` scheme so targets resolve through the service registry (Eureka) by
  `spring.application.name`, never hardcoded host/port.
- Every route is guarded by a Redis rate limiter and a circuit breaker with a fallback to
  `/fallback/{routeId}`.
- The gateway does not publish its own OpenAPI document; each service exposes
  `/v3/api-docs` and `/swagger-ui.html` on its own port.
