# Logical Data Model

Entity-relationship view of each database, normalized to 3NF. Cardinality is
expressed as one-to-one (1:1), one-to-many (1:N), many-to-many (N:M).
Cross-database references are **soft** (UUID column, no FK) and marked with an
asterisk (*).

## identity_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `permissions` | reference | N:M with `roles` via `role_permissions` |
| `users` | master | 1:N `user_sessions`, 1:N `password_history`, 1:N `mfa_devices`, N:M `roles` via `user_roles` |
| `roles` | master | N:M `permissions` via `role_permissions`, N:M `users` via `user_roles` |
| `user_roles` | bridge | `users` N:M `roles` |
| `role_permissions` | bridge | `roles` N:M `permissions` |
| `user_sessions` | transaction | N:1 `users` |
| `password_history` | history | N:1 `users` |
| `mfa_devices` | transaction | N:1 `users` |
| `users_history` | history | N:1 `users` |

- A user belongs to exactly one organization (`users.organization_id` 1:N `organizations` in organization_db, soft).
- A role belongs to one organization; `permissions` are a global catalog.

## organization_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `plans` | reference | 1:N `subscriptions` |
| `organizations` | master (tenant root) | 1:1 `organization_addresses`, 1:1 `subscriptions`, 1:N `organization_domains`, 1:N `departments`, 1:N `teams` |
| `organization_addresses` | detail | 1:1 `organizations` |
| `organization_domains` | detail | N:1 `organizations` |
| `subscriptions` | transaction | N:1 `organizations` (1:1 enforced by unique), N:1 `plans` |
| `departments` | master | N:1 `organizations`, 0..1 `parent_id` (self) |
| `teams` | master | N:1 `organizations`, N:1 `departments`, N:M `users*` via `team_members` |
| `team_members` | bridge | `teams` N:M `users*` |
| `organizations_history` | history | N:1 `organizations` |

- `organizations.id` is the platform tenant identifier referenced by every
  other database as `organization_id`.

## recruiter_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `recruiters` | master | 1:1 `recruiter_profiles`, 1:N `candidate_pipeline`, 1:N `recruiter_notes`, 1:N `recruiter_assignments` |
| `recruiter_profiles` | detail | 1:1 `recruiters` |
| `pipeline_stages` | reference | 1:N `candidate_pipeline` |
| `candidate_pipeline` | transaction | N:1 `recruiters`, N:1 `pipeline_stages`, N:1 `candidates*` |
| `recruiter_notes` | transaction | N:1 `recruiters`, N:1 `candidates*` |
| `recruiter_assignments` | transaction | N:1 `recruiters`, N:1 `candidates*` |
| `recruiters_history` | history | N:1 `recruiters` |

- A candidate may appear in several pipeline rows over time; the `CURRENT`
  status marks the active one.

## candidate_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `candidates` | master | 1:1 `candidate_profiles`, 1:N `candidate_documents`, 1:N `candidate_notes`, 1:N `assessments`, 1:N `candidate_consents`, N:M `tags` via `candidate_tags` |
| `candidate_profiles` | detail | 1:1 `candidates` |
| `candidate_documents` | transaction | N:1 `candidates`, N:1 `storage_objects*` |
| `candidate_notes` | transaction | N:1 `candidates`, N:1 `users*` (author) |
| `assessments` | transaction | N:1 `candidates` |
| `candidate_consents` | transaction | N:1 `candidates` (unique per `candidate_id, consent_type`) |
| `tags` | reference | N:M `candidates` via `candidate_tags` |
| `candidate_tags` | bridge | `candidates` N:M `tags` |
| `candidates_history` | history | N:1 `candidates` |

## interview_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `interviews` | master | 1:N `interview_sessions`, N:M `interviewers` via `interview_panels`, 1:N `interview_feedback`, 1:N `interview_calendar_events` |
| `interview_sessions` | transaction | N:1 `interviews` |
| `interviewers` | master | N:M `interviews` via `interview_panels`, 1:N `interview_feedback` |
| `interview_panels` | bridge | `interviews` N:M `interviewers` |
| `interview_feedback` | transaction | N:1 `interviews`, N:1 `interviewers` |
| `interview_calendar_events` | transaction | N:1 `interviews` |
| `interviews_history` | history | N:1 `interviews` |

- `candidate_id`/`recruiter_id` on `interviews` are soft references to
  candidate_db and recruiter_db. `interview_sessions.id` is the session
  referenced by telemetry and policy events.

## telemetry_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `telemetry_event_types` | reference | - |
| `telemetry_sessions` | master | 1:N `telemetry_events`, 1:N `telemetry_event_summaries` |
| `telemetry_events` | transaction (partitioned) | N:1 `telemetry_sessions` |
| `telemetry_event_summaries` | aggregate (partitioned) | N:1 `telemetry_sessions` |

- `interview_id` on sessions/events is a soft reference to
  interview_db. `telemetry_events` is RANGE-partitioned by month.

## policy_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `policies` | master | 1:N `policy_rules`, 1:N `policy_versions`, 1:N `violations` |
| `policy_rules` | detail | N:1 `policies` |
| `policy_versions` | history | N:1 `policies` |
| `violations` | transaction | N:1 `policies`, 1:N `violation_reviews`, 1:N `violation_escalations` |
| `violation_reviews` | transaction | N:1 `violations` |
| `violation_escalations` | transaction | N:1 `violations` |
| `policies_history` | history | N:1 `policies` |

- `violations.session_id` is a soft reference to telemetry sessions.

## audit_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `audit_events` | transaction (partitioned) | 1:N `audit_event_changes` |
| `audit_event_changes` | detail (partitioned) | N:1 `audit_events` |
| `api_audit_log` | transaction (partitioned) | - |

## notification_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `notification_templates` | reference | - |
| `notification_preferences` | detail | - |
| `notifications` | transaction | 1:N `notification_deliveries`, N:1 `users*` |
| `notification_deliveries` | transaction | N:1 `notifications` |

## report_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `reports` | master | 1:N `report_sections`, 1:1 `report_requests`, 1:N `report_schedules` (by type) |
| `report_sections` | detail | N:1 `reports` |
| `report_requests` | transaction | 1:1 `reports` |
| `report_schedules` | master | - |
| `reports_history` | history | N:1 `reports` |

## storage_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `storage_buckets` | master | 1:N `storage_objects` |
| `storage_objects` | master | N:1 `storage_buckets`, 1:N `object_versions`, 1:N `signed_urls` |
| `object_versions` | history | N:1 `storage_objects` |
| `signed_urls` | transaction | N:1 `storage_objects` |
| `storage_objects_history` | history | N:1 `storage_objects` |

## feature_flag_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `features` | master | 1:N `feature_flags`, 1:N `experiments` |
| `feature_flags` | detail | N:1 `features`, N:M `users*` via `flag_targets` |
| `flag_targets` | bridge | `feature_flags` N:M `users*` |
| `experiments` | transaction | N:1 `features` |
| `feature_flags_history` | history | N:1 `feature_flags` |

## scheduler_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `scheduled_jobs` | master | 1:N `job_executions`, 1:0..1 `job_locks` |
| `job_executions` | history | N:1 `scheduled_jobs` |
| `job_locks` | transaction | 1:1 `scheduled_jobs` |

## integration_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `integrations` | master | 1:N `integration_connections`, 1:N `integration_webhooks` |
| `integration_connections` | detail | N:1 `integrations`, 1:N `integration_sync_logs` |
| `integration_webhooks` | detail | N:1 `integrations` |
| `integration_sync_logs` | history | N:1 `integration_connections` |
| `integrations_history` | history | N:1 `integrations` |

## configuration_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `configuration_schema` | reference | - |
| `configurations` | master | 1:N `configuration_history` |
| `configuration_history` | history | N:1 `configurations` |

## analytics_db

| Entity | Type | Relationships |
|--------|------|---------------|
| `daily_organization_summaries` | aggregate | - |
| `daily_recruiter_summaries` | aggregate | - |
| `daily_candidate_summaries` | aggregate | - |
| `daily_interview_summaries` | aggregate | - |
| `daily_integrity_summaries` | aggregate | - |
| `weekly_*` views | view over daily | - |
| `mv_monthly_*` materialized views | view over daily | - |
| `analytics_job_runs` | history | - |

- `recruiter_id`/`candidate_id`/`interview_id` are soft references to their
  owning databases.
