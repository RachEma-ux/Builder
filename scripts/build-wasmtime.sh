#!/bin/bash

#
# Build Wasmtime for Android
#
# This script automates the process of building Wasmtime for Android ARM64 and x86_64.
# It handles cloning, building, and copying libraries to the Builder project.
#
# Usage:
#   ./scripts/build-wasmtime.sh [version]
#
# Example:
#   ./scripts/build-wasmtime.sh v15.0.0
#

set -e  # Exit on error

# Configuration
WASMTIME_VERSION="${1:-v15.0.0}"
ANDROID_API_LEVEL=26
BUILD_DIR="/tmp/wasmtime-build"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NATIVE_DIR="$PROJECT_ROOT/native/wasmtime-android"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check Rust
    if ! command -v cargo &> /dev/null; then
        log_error "Rust/Cargo not found. Install from https://rustup.rs/"
        exit 1
    fi

    # Check cargo-ndk
    if ! command -v cargo-ndk &> /dev/null; then
        log_warn "cargo-ndk not found. Installing..."
        cargo install cargo-ndk
    fi

    # Check Android NDK
    if [ -z "$ANDROID_NDK_HOME" ]; then
        log_error "ANDROID_NDK_HOME not set. Set it to your NDK directory."
        exit 1
    fi

    if [ ! -d "$ANDROID_NDK_HOME" ]; then
        log_error "ANDROID_NDK_HOME directory does not exist: $ANDROID_NDK_HOME"
        exit 1
    fi

    # Check Rust targets
    log_info "Checking Rust targets..."
    if ! rustup target list | grep -q "aarch64-linux-android (installed)"; then
        log_warn "Installing aarch64-linux-android target..."
        rustup target add aarch64-linux-android
    fi

    if ! rustup target list | grep -q "x86_64-linux-android (installed)"; then
        log_warn "Installing x86_64-linux-android target..."
        rustup target add x86_64-linux-android
    fi

    log_info "Prerequisites check passed!"
}

clone_wasmtime() {
    log_info "Cloning Wasmtime $WASMTIME_VERSION..."

    # Clean build directory
    if [ -d "$BUILD_DIR" ]; then
        log_warn "Removing existing build directory..."
        rm -rf "$BUILD_DIR"
    fi

    mkdir -p "$BUILD_DIR"
    cd "$BUILD_DIR"

    # Clone and checkout version
    git clone --depth 1 --branch "$WASMTIME_VERSION" \
        https://github.com/bytecodealliance/wasmtime.git

    cd wasmtime
    log_info "Cloned Wasmtime $(git describe --tags)"
}

build_for_arch() {
    local arch=$1
    local target=$2

    log_info "Building Wasmtime for $arch ($target)..."

    cd "$BUILD_DIR/wasmtime"

    cargo ndk \
        --target "$target" \
        --platform "$ANDROID_API_LEVEL" \
        build \
        --release \
        --manifest-path crates/c-api/Cargo.toml

    log_info "Build for $arch completed successfully!"
}

copy_libraries() {
    log_info "Copying libraries to Builder project..."

    # Create directories
    mkdir -p "$NATIVE_DIR/libs/arm64-v8a"
    mkdir -p "$NATIVE_DIR/libs/x86_64"
    mkdir -p "$NATIVE_DIR/include"

    cd "$BUILD_DIR/wasmtime"

    # Copy ARM64 library
    if [ -f "target/aarch64-linux-android/release/libwasmtime.so" ]; then
        cp target/aarch64-linux-android/release/libwasmtime.so \
           "$NATIVE_DIR/libs/arm64-v8a/"
        log_info "Copied ARM64 library"
    else
        log_error "ARM64 library not found!"
        exit 1
    fi

    # Copy x86_64 library
    if [ -f "target/x86_64-linux-android/release/libwasmtime.so" ]; then
        cp target/x86_64-linux-android/release/libwasmtime.so \
           "$NATIVE_DIR/libs/x86_64/"
        log_info "Copied x86_64 library"
    else
        log_error "x86_64 library not found!"
        exit 1
    fi

    # Copy headers
    if [ -f "crates/c-api/include/wasmtime.h" ]; then
        cp crates/c-api/include/wasmtime.h "$NATIVE_DIR/include/"
        cp crates/c-api/include/wasm.h "$NATIVE_DIR/include/"
        cp crates/c-api/include/wasmtime/*.h "$NATIVE_DIR/include/" 2>/dev/null || true
        log_info "Copied header files"
    else
        log_error "Header files not found!"
        exit 1
    fi

    # Print library sizes
    log_info "Library sizes:"
    ls -lh "$NATIVE_DIR/libs/arm64-v8a/libwasmtime.so"
    ls -lh "$NATIVE_DIR/libs/x86_64/libwasmtime.so"
}

strip_libraries() {
    log_info "Stripping debug symbols to reduce size..."

    local llvm_strip="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"

    if [ -f "$llvm_strip" ]; then
        $llvm_strip "$NATIVE_DIR/libs/arm64-v8a/libwasmtime.so"
        $llvm_strip "$NATIVE_DIR/libs/x86_64/libwasmtime.so"
        log_info "Libraries stripped"

        log_info "New library sizes:"
        ls -lh "$NATIVE_DIR/libs/arm64-v8a/libwasmtime.so"
        ls -lh "$NATIVE_DIR/libs/x86_64/libwasmtime.so"
    else
        log_warn "llvm-strip not found, skipping..."
    fi
}

create_version_file() {
    log_info "Creating version file..."

    cat > "$NATIVE_DIR/WASMTIME_VERSION" << EOF
Wasmtime Version: $WASMTIME_VERSION
Build Date: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
Android API Level: $ANDROID_API_LEVEL
Architectures: arm64-v8a, x86_64
EOF

    log_info "Version file created at $NATIVE_DIR/WASMTIME_VERSION"
}

cleanup() {
    log_info "Cleaning up build directory..."
    if [ -d "$BUILD_DIR" ]; then
        rm -rf "$BUILD_DIR"
    fi
    log_info "Cleanup complete"
}

main() {
    echo ""
    log_info "=== Wasmtime Build Script for Android ==="
    echo ""

    check_prerequisites
    clone_wasmtime
    build_for_arch "ARM64" "aarch64-linux-android"
    build_for_arch "x86_64" "x86_64-linux-android"
    copy_libraries
    strip_libraries
    create_version_file

    echo ""
    log_info "=== Build Complete! ==="
    log_info "Libraries installed in: $NATIVE_DIR/libs/"
    log_info "Headers installed in: $NATIVE_DIR/include/"
    log_info ""
    log_info "Next steps:"
    log_info "  1. Review CMakeLists.txt in native/wasmtime-android/"
    log_info "  2. Build Builder project: ./gradlew assembleDebug"
    log_info "  3. Test with a simple WASM module"
    echo ""

    # Cleanup only if successful
    read -p "Clean up build directory? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        cleanup
    else
        log_info "Build artifacts preserved in: $BUILD_DIR"
    fi
}

# Run main function
main
