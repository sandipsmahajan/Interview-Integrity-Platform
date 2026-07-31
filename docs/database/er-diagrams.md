# ER Diagrams

Mermaid `erDiagram` views per database. `||--||` = one-to-one,
`||--o{` = one-to-many, `}o--o{` = many-to-many. Soft cross-database
references are noted in comments and carry no FK edge.

## identity_db

```mermaid
erDiagram
    users ||--o{ user_sessions : "has"
    users ||--o{ password_history : "has"
    users ||--o{ mfa_devices : "registers"
    users }o--o{ roles : "via user_roles"
    roles }o--o{ permissions : "via role_permissions"
    users ||--o{ users_history : "audited"

    users {
        uuid id PK
        uuid organization_id
        text email
        text password_hash
        text display_name
        user_status status
        timestamptz created_at
        timestamptz updated_at
        bigint version
    }
    roles {
        uuid id PK
        uuid organization_id
        text code
        boolean is_system
    }
    permissions {
        uuid id PK
        text code
    }
    user_roles {
        uuid user_id PK
        uuid role_id PK
    }
    role_permissions {
        uuid role_id PK
        uuid permission_id PK
    }
    user_sessions {
        uuid id PK
        uuid user_id FK
        text refresh_token_hash
        session_status status
        timestamptz expires_at
    }
    mfa_devices {
        uuid id PK
        uuid user_id FK
        text kind
        text secret_ciphertext
    }
    password_history {
        uuid id PK
        uuid user_id FK
        text password_hash
    }
    users_history {
        bigserial history_id PK
        text history_action
        uuid id
    }
```

## organization_db

```mermaid
erDiagram
    organizations ||--|| organization_addresses : "has"
    organizations ||--|| subscriptions : "has"
    organizations ||--o{ organization_domains : "claims"
    organizations ||--o{ departments : "contains"
    plans ||--o{ subscriptions : "subscribes"
    departments }o--o{ departments : "parent"
    organizations ||--o{ teams : "contains"
    departments ||--o{ teams : "groups"
    teams }o--o{ users_soft : "members (soft)"
    organizations ||--o{ organizations_history : "audited"

    organizations {
        uuid id PK
        text name
        text slug
        organization_status status
        jsonb settings
        timestamptz created_at
        bigint version
    }
    plans {
        uuid id PK
        text code
        bigint monthly_price_cents
        int max_seats
        jsonb features
    }
    subscriptions {
        uuid id PK
        uuid organization_id FK
        uuid plan_id FK
        subscription_status status
        date current_period_start
        date current_period_end
    }
    departments {
        uuid id PK
        uuid organization_id FK
        uuid parent_id FK
        text name
    }
    teams {
        uuid id PK
        uuid organization_id FK
        uuid department_id FK
        text name
    }
    team_members {
        uuid team_id PK
        uuid user_id PK
    }
    organization_domains {
        uuid id PK
        uuid organization_id FK
        text domain
        timestamptz verified_at
    }
    organization_addresses {
        uuid id PK
        uuid organization_id FK
        text city
        char country_code
    }
```

## candidate_db

```mermaid
erDiagram
    candidates ||--|| candidate_profiles : "has"
    candidates ||--o{ candidate_documents : "owns"
    candidates ||--o{ candidate_notes : "receives"
    candidates ||--o{ assessments : "takes"
    candidates ||--o{ candidate_consents : "grants"
    candidates }o--o{ tags : "via candidate_tags"
    candidates ||--o{ candidates_history : "audited"

    candidates {
        uuid id PK
        uuid organization_id
        uuid user_id
        text email
        text full_name
        candidate_status status
        timestamptz created_at
        bigint version
    }
    candidate_profiles {
        uuid id PK
        uuid candidate_id FK
        text headline
        text skills_ar
        numeric experience_years
    }
    assessments {
        uuid id PK
        uuid candidate_id FK
        assessment_status status
        numeric score
        jsonb metadata
    }
    candidate_consents {
        uuid id PK
        uuid candidate_id FK
        text consent_type
        consent_status status
    }
    candidate_tags {
        uuid candidate_id PK
        uuid tag_id PK
    }
    tags {
        uuid id PK
        uuid organization_id
        text code
    }
    candidate_documents {
        uuid id PK
        uuid candidate_id FK
        uuid storage_object_id
        bigint size_bytes
    }
    candidate_notes {
        uuid id PK
        uuid candidate_id FK
        uuid author_id
        text body
    }
```

## interview_db

```mermaid
erDiagram
    interviews ||--o{ interview_sessions : "runs"
    interviews }o--o{ interviewers : "via interview_panels"
    interviews ||--o{ interview_feedback : "collects"
    interviews ||--o{ interview_calendar_events : "mirrors"
    interviewers ||--o{ interview_feedback : "writes"
    interviews ||--o{ interviews_history : "audited"

    interviews {
        uuid id PK
        uuid organization_id
        uuid candidate_id
        uuid recruiter_id
        int round_number
        interview_status status
        interview_mode mode
        timestamptz starts_at
        timestamptz ends_at
        bigint version
    }
    interview_sessions {
        uuid id PK
        uuid interview_id FK
        text session_token_hash
        session_status status
        int heartbeat_cadence_seconds
    }
    interviewers {
        uuid id PK
        uuid organization_id
        uuid user_id
        text full_name
        text email
    }
    interview_panels {
        uuid interview_id PK
        uuid interviewer_id PK
        text role
    }
    interview_feedback {
        uuid id PK
        uuid interview_id FK
        uuid interviewer_id FK
        int rating
        feedback_status status
    }
    interview_calendar_events {
        uuid id PK
        uuid interview_id FK
        text provider
        text provider_event_id
    }
```

## telemetry_db

```mermaid
erDiagram
    telemetry_sessions ||--o{ telemetry_events : "produces"
    telemetry_sessions ||--o{ telemetry_event_summaries : "rolls up"
    telemetry_event_types ||--o{ telemetry_events : "classifies"

    telemetry_sessions {
        uuid id PK
        uuid organization_id
        uuid interview_id
        telemetry_session_status status
        int heartbeat_cadence_seconds
        timestamptz started_at
    }
    telemetry_events {
        uuid id PK
        timestamptz occurred_at PK
        uuid organization_id
        uuid session_id
        text event_type
        bigint seq
        jsonb payload
    }
    telemetry_event_summaries {
        timestamptz bucket_start PK
        uuid organization_id PK
        uuid session_id PK
        text event_type PK
        bigint event_count
        jsonb last_payload
    }
    telemetry_event_types {
        uuid id PK
        text code
        int retention_days
    }
```

## policy_db

```mermaid
erDiagram
    policies ||--o{ policy_rules : "groups"
    policies ||--o{ policy_versions : "versions"
    policies ||--o{ violations : "triggers"
    violations ||--o{ violation_reviews : "reviewed"
    violations ||--o{ violation_escalations : "escalated"
    policies ||--o{ policies_history : "audited"

    policies {
        uuid id PK
        uuid organization_id
        text code
        policy_status status
        violation_severity default_severity
        boolean enabled
        bigint version
    }
    policy_rules {
        uuid id PK
        uuid policy_id FK
        text rule_code
        jsonb condition
        violation_severity severity
        int weight
    }
    policy_versions {
        uuid id PK
        uuid policy_id FK
        int version
        jsonb definition
    }
    violations {
        uuid id PK
        uuid organization_id
        uuid session_id
        text rule_code
        violation_severity severity
        violation_status status
        jsonb evidence
        timestamptz occurred_at
    }
    violation_reviews {
        uuid id PK
        uuid violation_id FK
        uuid reviewer_id
        review_action action
    }
    violation_escalations {
        uuid id PK
        uuid violation_id FK
        uuid escalated_to
    }
```

## audit_db

```mermaid
erDiagram
    audit_events ||--o{ audit_event_changes : "details"

    audit_events {
        uuid id PK
        timestamptz occurred_at PK
        uuid organization_id
        uuid actor_id
        text action
        text resource_type
        uuid resource_id
        audit_outcome outcome
        jsonb metadata
    }
    audit_event_changes {
        bigserial id PK
        timestamptz occurred_at PK
        uuid audit_event_id
        text field
        text old_value
        text new_value
    }
    api_audit_log {
        bigserial id PK
        timestamptz occurred_at PK
        uuid organization_id
        text method
        text path
        int status_code
    }
```

## notification_db / report_db / storage_db / feature_flag_db / scheduler_db / integration_db / configuration_db / analytics_db

```mermaid
erDiagram
    notifications ||--o{ notification_deliveries : "delivered via"

    notifications {
        uuid id PK
        uuid organization_id
        uuid user_id
        notification_channel channel
        notification_status status
        text body
    }
    notification_deliveries {
        bigserial id PK
        uuid notification_id FK
        text provider
        notification_status status
        int attempts
    }
```

```mermaid
erDiagram
    reports ||--o{ report_sections : "composes"
    reports ||--o{ report_requests : "requested by"
    reports ||--o{ reports_history : "audited"

    reports {
        uuid id PK
        uuid organization_id
        report_type type
        report_status status
        report_format format
        numeric score
        jsonb filters
    }
    report_sections {
        uuid id PK
        uuid report_id FK
        text section_type
        jsonb content
    }
    report_schedules {
        uuid id PK
        uuid organization_id
        report_type type
        text cron_expression
        jsonb recipients
    }
```

```mermaid
erDiagram
    storage_buckets ||--o{ storage_objects : "contains"
    storage_objects ||--o{ object_versions : "versions"
    storage_objects ||--o{ signed_urls : "grants"
    storage_objects ||--o{ storage_objects_history : "audited"

    storage_objects {
        uuid id PK
        uuid organization_id
        uuid bucket_id FK
        text key
        bigint size_bytes
        text checksum_sha256
        storage_class storage_class
        text storage_ref
    }
    signed_urls {
        uuid id PK
        uuid object_id FK
        url_purpose purpose
        text token_hash
        timestamptz expires_at
        int max_uses
    }
```

```mermaid
erDiagram
    features ||--o{ feature_flags : "configured as"
    features ||--o{ experiments : "measured by"
    feature_flags ||--o{ flag_targets : "targeted to"
    feature_flags ||--o{ feature_flags_history : "audited"

    features {
        uuid id PK
        uuid organization_id
        text code
        flag_kind kind
    }
    feature_flags {
        uuid id PK
        uuid feature_id FK
        text environment
        boolean enabled
        int rollout_percent
        jsonb variants
        jsonb rules
    }
    experiments {
        uuid id PK
        uuid feature_id FK
        experiment_status status
        text control_variant
        text treatment_variant
    }
```

```mermaid
erDiagram
    scheduled_jobs ||--o{ job_executions : "runs"
    scheduled_jobs ||--o| job_locks : "locked by"

    scheduled_jobs {
        uuid id PK
        uuid organization_id
        text name
        text cron_expression
        job_status status
        int max_retries
        timestamptz next_run_at
    }
    job_executions {
        uuid id PK
        uuid job_id FK
        execution_status status
        int exit_code
        text error_message
    }
    job_locks {
        uuid job_id PK
        text lock_token
        text owner_id
        timestamptz expires_at
    }
```

```mermaid
erDiagram
    integrations ||--o{ integration_connections : "connects"
    integrations ||--o{ integration_webhooks : "notifies"
    integration_connections ||--o{ integration_sync_logs : "logs"
    integrations ||--o{ integrations_history : "audited"

    integrations {
        uuid id PK
        uuid organization_id
        text provider
        integration_status status
        text credentials_ref
    }
    integration_connections {
        uuid id PK
        uuid integration_id FK
        text external_account_id
        integration_status status
        text scopes_ar
    }
    integration_webhooks {
        uuid id PK
        uuid integration_id FK
        text url
        text secret_hash
    }
```

```mermaid
erDiagram
    configurations ||--o{ configuration_history : "audited"
    configuration_schema ||--o{ configurations : "validates"

    configurations {
        uuid id PK
        uuid organization_id
        config_scope scope
        text key
        jsonb value
        bigint version
    }
    configuration_history {
        bigserial id PK
        uuid configuration_id
        text key
        jsonb old_value
        jsonb new_value
    }
```

```mermaid
erDiagram
    daily_organization_summaries ||--o{ mv_monthly_organization_summaries : "rolls up"
    daily_integrity_summaries ||--o{ mv_monthly_integrity_summaries : "rolls up"

    daily_organization_summaries {
        date summary_date PK
        uuid organization_id PK
        bigint interviews_scheduled
        bigint interviews_completed
        bigint violations
        numeric avg_integrity_score
    }
    daily_recruiter_summaries {
        date summary_date PK
        uuid organization_id PK
        uuid recruiter_id PK
        bigint interviews_held
        numeric avg_feedback_rating
    }
    daily_candidate_summaries {
        date summary_date PK
        uuid organization_id PK
        uuid candidate_id PK
        numeric avg_score
    }
    daily_interview_summaries {
        date summary_date PK
        uuid organization_id PK
        uuid interview_id PK
        numeric integrity_score
    }
    daily_integrity_summaries {
        date summary_date PK
        uuid organization_id PK
        bigint total_events
        jsonb violations_by_severity
        bigint sessions_started
    }
```
