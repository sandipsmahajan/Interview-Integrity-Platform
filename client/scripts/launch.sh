#!/usr/bin/env bash
# =============================================================================
# Integrity Pro Desktop Client - Linux/macOS Launcher
# =============================================================================
# Run this to start the Integrity Pro desktop client.
# integrity-pro will automatically start integrity-service if needed.
# =============================================================================
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"
exec "./integrity-pro"
