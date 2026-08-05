#!/usr/bin/env bash
# =============================================================================
# Integrity Pro Desktop Client - Release Package Script
# =============================================================================
# Builds both binaries and packages them into a distributable zip.
#
# Usage:
#   ./package-release.sh              # build for current OS
#   ./package-release.sh --target x86_64-pc-windows-gnu   # cross-compile
#   ./package-release.sh --target x86_64-unknown-linux-gnu
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CLIENT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ROOT="$(cd "$CLIENT_DIR/.." && pwd)"

TARGET="${1:-}"
if [[ "$TARGET" == "--target" ]]; then
  TARGET="$2"
  shift 2
fi

OUT_DIR="$CLIENT_DIR/dist"
RELEASE_DIR="$OUT_DIR/integrity-pro"
VERSION="0.1.0"

info()  { echo "[INFO]  $*"; }
error() { echo "[ERROR] $*"; }

# Determine target
if [[ -z "$TARGET" ]]; then
  TARGET=$(rustc -vV | grep host | awk '{print $2}')
  info "Building for host target: $TARGET"
else
  info "Building for target: $TARGET"
  if ! rustup target list --installed | grep -q "^$TARGET$"; then
    info "Installing target $TARGET ..."
    rustup target add "$TARGET"
  fi
fi

# Determine exe suffix and OS
case "$TARGET" in
  *windows*) EXE=".exe" ; PLATFORM="windows" ;;
  *apple*)   EXE=""      ; PLATFORM="macos" ;;
  *)         EXE=""      ; PLATFORM="linux" ;;
esac

PRO_EXE="integrity-pro${EXE}"
SVC_EXE="integrity-service${EXE}"
ZIP_NAME="integrity-pro-${VERSION}-${PLATFORM}.zip"

rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

# Build release binaries
info "Building integrity-service (release) ..."
cd "$CLIENT_DIR"
cargo build --release -p integrity-service ${TARGET:+--target "$TARGET"}

info "Building integrity-pro (release) ..."
cargo build --release -p integrity-pro ${TARGET:+--target "$TARGET"}

# Find built binaries
TARGET_DIR="$CLIENT_DIR/target/${TARGET:+${TARGET}/}release"
cp "$TARGET_DIR/$SVC_EXE" "$RELEASE_DIR/"
cp "$TARGET_DIR/$PRO_EXE" "$RELEASE_DIR/"

# Copy launch scripts
if [[ "$PLATFORM" == "windows" ]]; then
  cp "$SCRIPT_DIR/launch.bat" "$RELEASE_DIR/"
  LAUNCHER="launch.bat"
else
  cp "$SCRIPT_DIR/launch.sh" "$RELEASE_DIR/"
  chmod +x "$RELEASE_DIR/launch.sh"
  LAUNCHER="launch.sh"
fi

# Copy README
cp "$CLIENT_DIR/RUNNING.md" "$RELEASE_DIR/README.txt"

# Create zip
info "Creating $ZIP_NAME ..."
cd "$OUT_DIR"
if command -v zip &>/dev/null; then
  zip -r "$ZIP_NAME" "integrity-pro"
else
  # Use python's zipfile as fallback
  python3 -m zipfile -c "$ZIP_NAME" "integrity-pro"
fi

echo ""
info "Package created: $OUT_DIR/$ZIP_NAME"
info "Contents:"
find "$RELEASE_DIR" -type f | while read f; do
  echo "  ${f#$RELEASE_DIR/}"
done
