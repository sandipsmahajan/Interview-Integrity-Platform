#!/usr/bin/env bash
#
# Starts only the services required by the Recruiter portal
# (portals/recruiter) plus their mandatory dependencies:
#   1. Infrastructure  (postgres, redis, kafka, minio)  -> docker compose
#   2. discovery-service  (registry, port 8761)
#   3. api-gateway        (single entry point, port 8080)
#   4. recruiter portal services (parallel)
#
# This is a trimmed-down variant of run-services.sh. It starts only the
# services whose routes are exercised by the recruiter portal:
#   identity 8081 | organization 8082 | recruiter 8083 | candidate 8084
#   interview 8085 | policy-engine 8088 | report 8089 | notification 8090
#   audit 8092 | feature-flag 8094
# Services the portal does not call (telemetry, analytics, storage,
# scheduler, integration, configuration, desktop-client) are skipped.
#
# Usage:
#   scripts/run-recruiter-portal.sh              # starts portal services
#   scripts/run-recruiter-portal.sh --no-infra   # skip docker compose (infra already up)
#   scripts/run-recruiter-portal.sh --no-build   # skip gradle bootJar (reuse existing jars)
#   scripts/run-recruiter-portal.sh stop         # stop all started services
#   scripts/run-recruiter-portal.sh logs         # tail all service logs
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/build/logs"
PID_DIR="${ROOT_DIR}/build/pids"
COMPOSE_FILE="${ROOT_DIR}/infra/docker/docker-compose.yml"

BOOTSTRAP_SERVICES=(
  "discovery-service:8761"
  "api-gateway:8080"
)

RECRUITER_SERVICES=(
  "identity-service:8081"
  "organization-service:8082"
  "recruiter-service:8083"
  "candidate-service:8084"
  "interview-service:8085"
  "policy-engine-service:8088"
  "report-service:8089"
  "notification-service:8090"
  "audit-service:8092"
  "feature-flag-service:8094"
)

log()  { echo -e "\033[1;34m[run-recruiter-portal]\033[0m $*"; }
die()  { echo -e "\033[1;31m[run-recruiter-portal] ERROR: $*\033[0m" >&2; exit 1; }

mkdir -p "${LOG_DIR}" "${PID_DIR}"

# ---------------------------------------------------------------------------
# Find a Java 21 runtime (gradle toolchain provisions it into ~/.gradle/jdks)
# ---------------------------------------------------------------------------
find_java21() {
  local candidate
  for candidate in \
    "${HOME}/.gradle/jdks/"*/bin/java \
    /usr/lib/jvm/java-21-openjdk-amd64/bin/java \
    /usr/lib/jvm/temurin-21*/bin/java \
    /opt/java/*/bin/java; do
    if [ -x "${candidate}" ]; then
      "${candidate}" -version 2>&1 | grep -q '"21' && { echo "${candidate}"; return 0; }
    fi
  done
  command -v java >/dev/null 2>&1 && { java -version 2>&1 | grep -q '"21' && { command -v java; return 0; }; }
  return 1
}

JAVA_BIN="$(find_java21 || true)"
if [ -z "${JAVA_BIN}" ]; then
  log "No JDK 21 found; gradle will provision one during the build."
fi

# ---------------------------------------------------------------------------
# Health check
# ---------------------------------------------------------------------------
wait_for_health() {
  local name="$1" port="$2" timeout="${3:-120}"
  local deadline=$(( $(date +%s) + timeout ))
  local url="http://localhost:${port}/actuator/health"
  until [ "$(date +%s)" -ge "${deadline}" ]; do
    if curl -sf "${url}" 2>/dev/null | grep -q '"UP"'; then
      log "${name} is UP (${url})"
      return 0
    fi
    sleep 2
  done
  die "${name} did not become healthy within ${timeout}s (see ${LOG_DIR}/${name}.log)"
}

start_one() {
  local name="$1" port="$2" jar
  jar="${ROOT_DIR}/services/${name}/build/libs/${name}-0.1.0.jar"
  [ -f "${jar}" ] || die "Missing jar ${jar}; run with build step enabled."
  if [ -n "${JAVA_BIN}" ]; then
    nohup "${JAVA_BIN}" -jar "${jar}" >"${LOG_DIR}/${name}.log" 2>&1 &
  else
    nohup java -jar "${jar}" >"${LOG_DIR}/${name}.log" 2>&1 &
  fi
  echo "$!" > "${PID_DIR}/${name}.pid"
  log "Started ${name} (pid $(cat "${PID_DIR}/${name}.pid"), port ${port})"
}

stop_all() {
  log "Stopping all services..."
  for pidfile in "${PID_DIR}"/*.pid; do
    [ -e "${pidfile}" ] || continue
    local pid
    pid="$(cat "${pidfile}")"
    if kill -0 "${pid}" 2>/dev/null; then
      kill "${pid}" 2>/dev/null || true
      wait "${pid}" 2>/dev/null || true
    fi
    rm -f "${pidfile}"
  done
  log "All services stopped."
}

# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------
case "${1:-start}" in
  stop)
    stop_all
    exit 0
    ;;
  logs)
    # shellcheck disable=SC1090
    tail -n 100 -f "${LOG_DIR}"/*.log
    exit 0
    ;;
esac

NO_INFRA=false
NO_BUILD=false
for arg in "$@"; do
  case "${arg}" in
    --no-infra) NO_INFRA=true ;;
    --no-build) NO_BUILD=true ;;
    *) die "Unknown argument: ${arg}" ;;
  esac
done

# ---------------------------------------------------------------------------
# 1. Infrastructure
# ---------------------------------------------------------------------------
if [ "${NO_INFRA}" = false ]; then
  log "Starting infrastructure (postgres, redis, kafka, minio)..."
  [ -f "${COMPOSE_FILE}" ] || die "compose file not found: ${COMPOSE_FILE}"
  docker compose -f "${COMPOSE_FILE}" up -d postgres redis kafka minio
  log "Waiting for postgres health..."
  until docker compose -f "${COMPOSE_FILE}" exec -T postgres pg_isready -U integrity -d postgres >/dev/null 2>&1; do sleep 2; done
  log "Infrastructure is up."
else
  log "Skipping infrastructure (--no-infra)."
fi

# ---------------------------------------------------------------------------
# 2. Build bootJars for the recruiter portal set
# ---------------------------------------------------------------------------
if [ "${NO_BUILD}" = false ]; then
  log "Building bootJars for recruiter portal services..."
  local_build_tasks=""
  for entry in "${BOOTSTRAP_SERVICES[@]}" "${RECRUITER_SERVICES[@]}"; do
    name="${entry%%:*}"
    jar="${ROOT_DIR}/services/${name}/build/libs/${name}-0.1.0.jar"
    if [ ! -f "${jar}" ]; then
      local_build_tasks="${local_build_tasks} :services:${name}:bootJar"
    fi
  done
  if [ -n "${local_build_tasks}" ]; then
    # shellcheck disable=SC2086
    (cd "${ROOT_DIR}" && ./gradlew ${local_build_tasks} --console=plain) || die "gradle build failed"
  else
    log "All bootJars already present; skipping build."
  fi
  JAVA_BIN="${JAVA_BIN:-$(find_java21 || true)}"
  [ -z "${JAVA_BIN}" ] && die "No JDK 21 available to run the services."
  log "Using JDK: ${JAVA_BIN}"
else
  log "Skipping build (--no-build)."
fi

# ---------------------------------------------------------------------------
# 3. Bootstrap services first (registry then gateway)
# ---------------------------------------------------------------------------
for entry in "${BOOTSTRAP_SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  start_one "${name}" "${port}"
done

for entry in "${BOOTSTRAP_SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  wait_for_health "${name}" "${port}"
done

# ---------------------------------------------------------------------------
# 4. Recruiter portal services (parallel)
# ---------------------------------------------------------------------------
for entry in "${RECRUITER_SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  start_one "${name}" "${port}"
done

log "All recruiter portal services launched. Waiting for them to become healthy..."
for entry in "${RECRUITER_SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  wait_for_health "${name}" "${port}" 180
done

log ""
log "=========================================================================="
log "  Recruiter portal stack is UP."
log "  Gateway:            http://localhost:8080"
log "  Discovery:          http://localhost:8761"
log "  Portal UI (dev):    http://localhost:5173  (npm --prefix portals/recruiter run dev)"
log "  Logs:               ${LOG_DIR}"
log "  Stop everything:    scripts/run-recruiter-portal.sh stop"
log "=========================================================================="
