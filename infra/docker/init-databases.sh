#!/usr/bin/env bash
# Creates the per-service databases on the first boot of the postgres container.
# Mounted at /docker-entrypoint-initdb.d/ so it runs once after the postgres
# image initializes the cluster.

set -euo pipefail

DATABASES=(
  identity_db
  organization_db
  recruiter_db
  candidate_db
  interview_db
  telemetry_db
  policy_db
  report_db
  notification_db
  analytics_db
  audit_db
  storage_db
  feature_flag_db
  scheduler_db
  integration_db
  configuration_db
)

for DB in "${DATABASES[@]}"; do
  if ! psql -v ON_ERROR_STOP=1 -d postgres -U "$POSTGRES_USER" -tAc "SELECT 1 FROM pg_database WHERE datname='${DB}'" | grep -q 1; then
    psql -v ON_ERROR_STOP=1 -d postgres -U "$POSTGRES_USER" -c "CREATE DATABASE ${DB};"
  fi
done
