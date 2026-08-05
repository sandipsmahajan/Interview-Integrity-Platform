#!/usr/bin/env bash
# =============================================================================
# Integrity Pro Desktop Client - Development Start Script
# =============================================================================
# Starts the infrastructure + backend services needed by the desktop client,
# then prints instructions for running the Tauri/Rust desktop app.
#
# Usage:
#   ./scripts/start-desktop-services.sh          # full Docker Compose
#   ./scripts/start-desktop-services.sh --gradle # infra in Docker, services via Gradle
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; }
step()  { echo -e "\n${CYAN}=== $* ===${NC}"; }

# ---------------------------------------------------------------------------
# 1.  Infrastructure (Docker Compose — infrastructure profile only)
# ---------------------------------------------------------------------------
step "Starting infrastructure (PostgreSQL, Redis, Kafka, MinIO, Mailpit)"
cd "$ROOT"

docker compose -f infra/docker/docker-compose.yml up -d --wait \
  postgres redis kafka minio mailpit 2>&1

info "Infrastructure is ready."
echo "  PostgreSQL : localhost:5432 (user integrity / password integrity)"
echo "  Redis      : localhost:6379"
echo "  Kafka      : localhost:9092"
echo "  MinIO      : localhost:9000 (console :9001)"
echo "  Mailpit    : localhost:1025 (web UI :8025)"

# ---------------------------------------------------------------------------
# 2.  Service registry
# ---------------------------------------------------------------------------
step "Building essential services"
cd "$ROOT"

# The 8 services required by the desktop client.
SERVICES=(
  discovery-service
  api-gateway
  identity-service
  interview-service
  telemetry-service
  policy-engine-service
  desktop-client-service
  configuration-service
)

if [[ "${1:-}" == "--gradle" ]]; then

  # --- Gradle-based runner (run services in background terminals) ---
  info "Starting services via Gradle (local profile) ..."

  for svc in "${SERVICES[@]}"; do
    info "Starting $svc ..."
    SPRING_PROFILES_ACTIVE=local \
    SPRING_CONFIG_LOCATION=infra/config/ \
    ./gradlew "services/$svc:bootRun" &
    sleep 3
  done

  wait

else

  # --- Full Docker Compose (infrastructure already running) ---
  info "Building & starting all services via Docker Compose ..."
  docker compose -f infra/docker/docker-compose.yml up -d --build 2>&1

fi

# ---------------------------------------------------------------------------
# 3.  Wait for services to be healthy
# ---------------------------------------------------------------------------
step "Waiting for services to become healthy"

wait_for_url() {
  local url="$1" label="$2" max="${3:-120}"
  local elapsed=0
  while ! curl -sf -o /dev/null "$url" 2>/dev/null; do
    sleep 2
    elapsed=$((elapsed + 2))
    if [[ $elapsed -ge $max ]]; then
      error "$label not reachable after ${max}s"
      return 1
    fi
  done
  info "$label is healthy ($url)"
}

wait_for_url "http://localhost:8761/actuator/health" "discovery-service"
wait_for_url "http://localhost:8080/actuator/health"    "api-gateway"
wait_for_url "http://localhost:8081/actuator/health"    "identity-service"
wait_for_url "http://localhost:8085/actuator/health"    "interview-service"
wait_for_url "http://localhost:8087/actuator/health"    "telemetry-service"
wait_for_url "http://localhost:8088/actuator/health"    "policy-engine-service"
wait_for_url "http://localhost:8086/actuator/health"    "desktop-client-service"
wait_for_url "http://localhost:8097/actuator/health"    "configuration-service"

echo ""
info "All 8 backend services are healthy."

# ---------------------------------------------------------------------------
# 4.  Print desktop client instructions
# ---------------------------------------------------------------------------
cat <<'EOF'

╔══════════════════════════════════════════════════════════════════════════╗
║                      DESKTOP CLIENT — HOW TO RUN                        ║
╠══════════════════════════════════════════════════════════════════════════╣
║                                                                          ║
║  Prerequisites:                                                          ║
║    - Rust toolchain (rustup: https://rustup.rs)                          ║
║    - Tauri v2 dependencies (see https://v2.tauri.app/start/prerequisites)║
║    - Node.js >= 18                                                       ║
║                                                                          ║
║  Steps:                                                                  ║
║                                                                          ║
║  1. Install frontend dependencies (one-time):                            ║
║     cd client/desktop-app/web && npm install                             ║
║                                                                          ║
║  2. Run the desktop client in dev mode (from workspace root):            ║
║     cd client/desktop-app                                                ║
║     cargo tauri dev                                                      ║
║                                                                          ║
║     This starts the Vite dev server on :1420 and launches the Tauri      ║
║     window. The client connects to the API gateway at localhost:8080.    ║
║                                                                          ║
║  3. (Alternative) Build the production binary:                           ║
║     cd client/desktop-app                                                ║
║     cargo tauri build                                                    ║
║                                                                          ║
║  4. (Alternative) Run in headless dev mode (Vite only, no Tauri):        ║
║     cd client/desktop-app/web && npm run dev                             ║
║     → Opens http://localhost:1420 in a browser (no native features).     ║
║                                                                          ║
║  Environment variables:                                                  ║
║    INTEGRITY_API_URL     Backend API base URL (default: http://localhost:8080)║
║    RUST_LOG              Tracing level (default: info)                   ║
║                                                                          ║
║  Tauri v2 system dependencies (Linux):                                   ║
║    apt install libwebkit2gtk-4.1-dev libgtk-3-dev libayatana-appindicator3-dev║
║    librsvg2-dev libjavascriptcoregtk-4.1-dev libsoup-3.0-dev             ║
║                                                                          ║
║  Tauri v2 system dependencies (Windows):                                 ║
║    - Microsoft Visual Studio C++ Build Tools                             ║
║    - WebView2 runtime (preinstalled on Win 10+)                          ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
EOF
