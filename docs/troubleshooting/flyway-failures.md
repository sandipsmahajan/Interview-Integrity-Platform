# Troubleshooting: Flyway Failures

**Symptom.** A service fails to start with a Flyway exception, or a migration is half-applied.

## 1. `Validate failed: Migration checksum mismatch for migration version 3`

```text
Caused by: org.flywaydb.core.api.FlywayException: Validate failed:
Migration checksum mismatch for migration version 3
Applied to database : x but resolved locally : y
```

**Cause:** you edited a migration file (`V3__*.sql`) **after** it was already applied to a
database. Flyway protects you: silently "fixing" an applied migration is how data gets corrupted.

**Diagnose:**

```bash
# See what's recorded vs what's on disk
kubectl -n integrity exec -it postgres-0 -- psql -U integrity -d identity_db -c \
  "SELECT installed_rank, version, description, success, checksum FROM flyway_schema_history ORDER BY installed_rank;"
```

**Fix (correct paths only):**

1. **If the migration was already applied to prod/uat/qa:** do **not** edit it. Add a *new*
   migration (`V4__fix.sql`) that applies the correction. Revert your local edit.
2. **If it was applied only in dev/local and you truly want to change it:**
   - Option A (clean slate): drop the database and let all migrations re-run:
     ```bash
     # dev/local only!
     kubectl -n integrity exec -it postgres-0 -- psql -U integrity -d postgres \
       -c "DROP DATABASE identity_db;" -c "CREATE DATABASE identity_db;"
     kubectl -n integrity rollout restart deployment/identity-service
     ```
   - Option B (repair checksums — only when you know what you're doing):
     ```bash
     # runs against the DB via the service context
     # (Flyway repair rewrites the history table; use rarely)
     ```

## 2. `Schema history table flyway_schema_history ... doesn't exist` / `outdated`

**Cause:** first run (fine) or the schema history was deleted/restored from an older backup.

**Fix:** if restoring from backup, the history table comes with it — ensure the restored backup
includes it (the daily snapshot does). Do not hand-create the history table.

## 3. Migration fails partway (`success = false` row)

```text
Migration V4__... failed
```

**Cause:** SQL error mid-migration. Flyway marks the version failed and rolls the transaction
back (PostgreSQL migrations are transactional).

**Diagnose:**

```bash
kubectl -n integrity logs deploy/<service> --tail=60 | grep -A5 'Migration'
kubectl -n integrity exec -it postgres-0 -- psql -U integrity -d <db> -c \
  "SELECT version, description, success FROM flyway_schema_history WHERE success = false;"
```

**Fix:**

- Fix the SQL, **bump the version** (`V5__...`) with the corrected logic, and restart — never
  rerun the same version.
- If the failed version must be retired, mark it as ignored or repair the history table with
  explicit review.

## 4. `Detected resolved migration not applied to database` (out-of-order)

**Cause:** a new migration appeared on disk that wasn't on the older backup you restored, or
`outOfOrder` is disabled and someone applied things in a different order.

**Fix:**

- Prefer keeping all environments' migrations in lockstep (same commit).
- For a deliberate hotfix on one env, enable `spring.flyway.out-of-order=true` **only**
  temporarily, apply, then revert.

## 5. Services start before migrations finish

**Symptom:** readiness passes but data is incomplete.

**Cause:** Flyway runs at startup; if the app serves before migration completes you'd see missing
tables. In this platform Flyway runs before the web server starts, so this should not happen. If
it does, check `spring.flyway.enabled` wasn't overridden and that the startup log shows the
migration lines **before** "Started ...".

## Prevention

- **Never edit an applied migration.** New versions only, in the same commit as their code.
- Review migration SQL in PRs (a DDL change is a code change).
- Test migrations on a copy of prod data quarterly (restore drill doubles as a migration test).
- Watch the deploy logs for "FlywayException" as a deploy gate (the pipeline smoke test covers
  startup health).
