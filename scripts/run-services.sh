#!/usr/bin/env bash
#
# Starts the Interview Integrity platform in the correct order:
#   1. Infrastructure  (postgres, redis, kafka, minio)  -> docker compose
#   2. discovery-service  (registry, port 8761)
#   3. api-gateway        (single entry point, port 8080)
#   4. domain services    (ports 8081-8097, started in parallel)
#
# Each service is health-checked via /actuator/health before the next stage
# proceeds. Logs go to build/logs/<service>.log.
#
# Usage:
#   scripts/run-services.sh              # full stack (builds jars if needed)
#   scripts/run-services.sh --no-infra   # skip docker compose (infra already up)
#   scripts/run-services.sh --no-build   # skip gradle bootJar (reuse existing jars)
#   scripts/run-services.sh stop         # stop all started services
#   scripts/run-services.sh logs         # tail all service logs
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${ROOT_DIR}/build/logs"
PID_DIR="${ROOT_DIR}/build/pids"
COMPOSE_FILE="${ROOT_DIR}/infra/docker/docker-compose.yml"

SERVICES=(
  "discovery-service:8761"
  "api-gateway:8080"
  "identity-service:8081"
  "organization-service:8082"
  "recruiter-service:8083"
  "candidate-service:8084"
  "interview-service:8085"
  "desktop-client-service:8086"
  "telemetry-service:8087"
  "policy-engine-service:8088"
  "report-service:8089"
  "notification-service:8090"
  "analytics-service:8091"
  "audit-service:8092"
  "storage-service:8093"
  "feature-flag-service:8094"
  "scheduler-service:8095"
  "integration-service:8096"
  "configuration-service:8097"
)

# Services that need a reachable registry before they can start meaningfully.
INFRA_DEPENDENT_ONLY=true
BOOTSTRAP_SERVICES=("discovery-service:8761" "api-gateway:8080")

log()  { echo -e "\033[1;34m[run-services]\033[0m $*"; }
die()  { echo -e "\033[1;31m[run-services] ERROR: $*\033[0m" >&2; exit 1; }

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
  local java_args=(
    "-jar" "${jar}"
    "-Dspring.profiles.active=local"
    "-Dspring.config.additional-location=${ROOT_DIR}/infra/config/"
  )
  if [ -n "${JAVA_BIN}" ]; then
    nohup "${JAVA_BIN}" "${java_args[@]}" >"${LOG_DIR}/${name}.log" 2>&1 &
  else
    nohup java "${java_args[@]}" >"${LOG_DIR}/${name}.log" 2>&1 &
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
# 2. Build bootJars
# ---------------------------------------------------------------------------
if [ "${NO_BUILD}" = false ]; then
  log "Building bootJars for all services (first run provisions JDK 21)..."
  local_build_tasks=""
  for entry in "${SERVICES[@]}"; do
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
# 4. Domain services (parallel)
# ---------------------------------------------------------------------------
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  case "${name}" in
    discovery-service|api-gateway) continue ;;
  esac
  start_one "${name}" "${port}"
done

log "All services launched. Waiting for them to become healthy..."
FAILED=""
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  if ! wait_for_health "${name}" "${port}" 180; then
    FAILED="${FAILED} ${name}"
  fi
done

if [ -n "${FAILED}" ]; then
  die "The following services failed health checks:${FAILED}"
fi

log ""
log "=========================================================================="
log "  All services are UP."
log "  Gateway:            http://localhost:8080"
log "  Discovery:          http://localhost:8761"
log "  Logs:               ${LOG_DIR}"
log "  Stop everything:    scripts/run-services.sh stop"
log "=========================================================================="
