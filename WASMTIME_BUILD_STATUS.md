# Wasmtime Build Status Report

**Date**: 2026-01-10
**Environment**: Containerized build environment
**Status**: ⚠️ **NDK Required** - Prerequisites partially met

---

## Environment Check Results

### ✅ Prerequisites Met

| Component | Status | Version | Notes |
|-----------|--------|---------|-------|
| Rust/Cargo | ✅ Installed | 1.92.0 | ✅ Exceeds minimum (1.70+) |
| cargo-ndk | ✅ Installed | 4.1.2 | ✅ Build tool ready |
| aarch64-linux-android | ✅ Installed | - | ✅ ARM64 target ready |
| x86_64-linux-android | ✅ Installed | - | ✅ x86_64 target ready |

### ❌ Missing Prerequisites

| Component | Status | Required For | Solution |
|-----------|--------|--------------|----------|
| Android NDK | ❌ Not Found | Native compilation | Install Android NDK r25+ |
| ANDROID_NDK_HOME | ❌ Not Set | Build script | Set environment variable |

---

## What This Means

### Good News ✅

**Rust toolchain is fully configured!**
- Rust 1.92.0 is installed (latest stable, exceeds minimum 1.70+)
- cargo-ndk build tool is ready
- Both Android targets (ARM64, x86_64) are installed
- Build script is present and ready to use

### Blocker ❌

**Android NDK is not installed**
- The Wasmtime build requires Android NDK r25 or newer
- NDK provides native compilation toolchain for Android
- Build script will exit with error: "ANDROID_NDK_HOME not set"

---

## Next Steps

### Option 1: Build on Your Local Machine (Recommended)

The WASM build must be done on a machine with Android NDK installed.

**Prerequisites Installation:**

```bash
# 1. Install Android Studio (includes SDK Manager)
# Download from: https://developer.android.com/studio

# 2. Open Android Studio > Tools > SDK Manager
# Install:
#   - Android SDK Platform (API 26-34)
#   - Android SDK Build-Tools
#   - NDK (Side by side) - version 25.2.9519653 or newer

# 3. Set environment variable
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.2.9519653

# Or add to ~/.bashrc or ~/.zshrc:
echo 'export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.2.9519653' >> ~/.bashrc
source ~/.bashrc
```

**Run the Build:**

```bash
# Navigate to project
cd Builder

# Run automated build script
./scripts/build-wasmtime.sh v15.0.0

# This will:
# 1. Check all prerequisites ✅
# 2. Clone Wasmtime v15.0.0 from GitHub
# 3. Build for ARM64 (aarch64-linux-android)
# 4. Build for x86_64 (x86_64-linux-android)
# 5. Copy libraries to native/wasmtime-android/libs/

# Expected time: 2-3 hours (first build)
# Output:
#   native/wasmtime-android/libs/arm64-v8a/libwasmtime.so
#   native/wasmtime-android/libs/x86_64/libwasmtime.so
```

**Rebuild the App:**

```bash
# Clean build to pick up new native libraries
./gradlew clean assembleDebug

# The app will now have full WASM support
```

### Option 2: Skip WASM Build (App Still Works!)

**You can use the app without building Wasmtime!**

The Builder app has two pack runtime modes:

1. **Workflow Engine** (No WASM needed) ✅
   - HTTP requests
   - KV store operations
   - Logging
   - Sleep/delay
   - **Fully functional right now!**

2. **WASM Runtime** (Requires Wasmtime) ⚠️
   - Execute .wasm pack binaries
   - Full WASM isolation
   - **Optional - only needed for WASM packs**

**What works without WASM:**
- ✅ Build the app: `./gradlew assembleDebug`
- ✅ Install packs from GitHub (Workflow-based packs)
- ✅ OAuth authentication
- ✅ Logs and health monitoring
- ✅ Instance management
- ✅ Workflow execution (HTTP, KV, logging, sleep)

**What requires WASM:**
- ❌ Execute .wasm pack binaries
- ❌ Install WASM-based packs

---

## Build Script Capabilities

The automated script `scripts/build-wasmtime.sh` handles:

### Automated Steps

1. **Prerequisites Check**
   - Validates Rust installation
   - Checks cargo-ndk availability
   - Verifies ANDROID_NDK_HOME is set
   - Confirms NDK directory exists
   - Checks/installs Android targets

2. **Wasmtime Clone**
   - Clones from GitHub (https://github.com/bytecodealliance/wasmtime)
   - Checks out specified version (default: v15.0.0)
   - Verifies git checkout success

3. **ARM64 Build**
   ```bash
   cargo ndk \
     --target aarch64-linux-android \
     --platform 26 \
     build --release \
     --manifest-path crates/c-api/Cargo.toml
   ```
   - Builds for ARM64 devices (most Android phones/tablets)
   - API level 26+ (Android 8.0+)
   - Release mode with optimizations

4. **x86_64 Build**
   ```bash
   cargo ndk \
     --target x86_64-linux-android \
     --platform 26 \
     build --release \
     --manifest-path crates/c-api/Cargo.toml
   ```
   - Builds for x86_64 emulators
   - Same API level and optimizations

5. **Library Installation**
   - Creates `native/wasmtime-android/libs/arm64-v8a/` directory
   - Creates `native/wasmtime-android/libs/x86_64/` directory
   - Copies `libwasmtime.so` to both locations
   - Verifies library sizes and integrity

6. **Verification**
   - Checks library files exist
   - Validates file sizes (typically ~15-25 MB per .so)
   - Reports success/failure

---

## Technical Details

### Why NDK is Required

Wasmtime is written in Rust and compiles to native code. To run on Android:

1. **Cross-compilation**: Rust code must be compiled for Android's Linux kernel
2. **System libraries**: NDK provides Android-specific libc, libm, etc.
3. **ABI compatibility**: NDK ensures ARM64/x86_64 Android ABI compatibility
4. **JNI bridge**: NDK headers enable Java ↔ Rust communication

### Build Output

After successful build, you'll have:

```
native/wasmtime-android/
├── libs/
│   ├── arm64-v8a/
│   │   └── libwasmtime.so     (~18 MB)
│   └── x86_64/
│       └── libwasmtime.so     (~20 MB)
├── src/main/cpp/
│   ├── wasmtime_jni.cpp       (JNI bridge code)
│   └── wasmtime_jni.h         (Header file)
└── CMakeLists.txt             (Build configuration)
```

### Integration

The `CMakeLists.txt` file is already configured to:
- **Detect** if `libwasmtime.so` exists
- **Link** if found, enabling WASM features
- **Skip** if not found, disabling WASM features
- **Compile** JNI bridge (`wasmtime_jni.cpp`)

No code changes needed - just build!

---

## Current Environment Limitations

### Why We Can't Build Here

This containerized environment has:
- ✅ Rust toolchain (1.92.0)
- ✅ cargo-ndk (4.1.2)
- ✅ Android targets (ARM64, x86_64)
- ❌ **No Android NDK**
- ❌ Network restrictions (may block git clone)

### What You Have on Local Machine

Your local development machine likely has:
- ✅ Android Studio with NDK
- ✅ Unrestricted internet (for git clone)
- ✅ More RAM/CPU for faster builds
- ✅ Persistent build cache (faster rebuilds)

---

## Estimated Build Times

| Build Type | First Build | Cached Rebuild | Notes |
|------------|-------------|----------------|-------|
| Wasmtime ARM64 | 60-90 min | 2-5 min | Most time on first build |
| Wasmtime x86_64 | 60-90 min | 2-5 min | Similar to ARM64 |
| **Total WASM** | **2-3 hours** | **5-10 min** | One-time setup |
| Builder APK (after WASM) | 5-10 min | 30-60 sec | Standard Android build |

**Note**: Times vary based on:
- CPU cores (more = faster)
- RAM available (16GB+ recommended)
- SSD vs HDD (SSD 3-5x faster)
- Network speed (for initial clone)

---

## Troubleshooting

### "ANDROID_NDK_HOME not set"

**Problem**: Environment variable not configured

**Solution**:
```bash
# Find your NDK path
ls ~/Android/Sdk/ndk/
# Output: 25.2.9519653  26.1.10909125  (or similar)

# Use the latest version
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.2.9519653

# Verify
echo $ANDROID_NDK_HOME
ls $ANDROID_NDK_HOME  # Should show: build, meta, ndk-build, etc.
```

### "Directory does not exist"

**Problem**: NDK path is wrong or NDK not installed

**Solution**:
```bash
# Install via Android Studio:
# Tools > SDK Manager > SDK Tools tab
# Check "NDK (Side by side)" > Apply

# Or install via command line:
sdkmanager --install "ndk;25.2.9519653"
```

### "cargo-ndk: command not found"

**Problem**: cargo-ndk not in PATH

**Solution**:
```bash
# Install cargo-ndk
cargo install cargo-ndk

# Verify
cargo-ndk --version
```

### "error: target 'aarch64-linux-android' not found"

**Problem**: Rust target not installed

**Solution**:
```bash
# Add both targets
rustup target add aarch64-linux-android
rustup target add x86_64-linux-android

# Verify
rustup target list | grep android
```

### Build hangs or fails with "killed"

**Problem**: Out of memory

**Solution**:
- Close other applications
- Increase swap space
- Use fewer parallel jobs: `export CARGO_BUILD_JOBS=2`

### "error: linker `aarch64-linux-android-gcc` not found"

**Problem**: NDK not properly detected

**Solution**:
```bash
# Verify NDK has required files
ls $ANDROID_NDK_HOME/toolchains/llvm/prebuilt/*/bin/

# Should see:
# aarch64-linux-android26-clang
# x86_64-linux-android26-clang
```

---

## Summary

### Current Status

| Item | Status | Action Required |
|------|--------|-----------------|
| Rust Toolchain | ✅ Ready (1.92.0) | None - already configured |
| cargo-ndk | ✅ Ready (4.1.2) | None - already configured |
| Android Targets | ✅ Ready | None - already configured |
| Build Script | ✅ Ready | None - already configured |
| Android NDK | ❌ Missing | **Install on local machine** |
| Build Environment | ⚠️ Limited | **Use local machine** |

### Recommendation

**For WASM support:**
1. Install Android NDK on your local machine (via Android Studio)
2. Set `ANDROID_NDK_HOME` environment variable
3. Run `./scripts/build-wasmtime.sh v15.0.0`
4. Rebuild app: `./gradlew clean assembleDebug`
5. Enjoy full WASM pack support! 🎉

**For immediate development:**
1. Skip WASM build
2. Build app as-is: `./gradlew assembleDebug`
3. Use Workflow-based packs (no WASM needed)
4. Add WASM support later when needed

---

## Documentation

For detailed instructions, see:

- **Setup Guide**: `docs/WASMTIME_BUILD_GUIDE.md` (400+ lines)
  - Complete NDK installation steps
  - Troubleshooting for all platforms (Linux, macOS, Windows)
  - Advanced configuration options

- **Build Script**: `scripts/build-wasmtime.sh` (250+ lines)
  - Automated build process
  - Error handling and validation
  - Progress reporting

- **Native Integration**: `native/wasmtime-android/README.md`
  - JNI bridge API documentation
  - Example usage
  - Error handling

---

## Quick Start Commands

### On Your Local Machine

```bash
# 1. Install Android Studio + NDK
# https://developer.android.com/studio

# 2. Set NDK path
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.2.9519653

# 3. Clone and build
git clone https://github.com/RachEma-ux/Builder.git
cd Builder
./scripts/build-wasmtime.sh v15.0.0

# 4. Rebuild app with WASM support
./gradlew clean assembleDebug

# Done! 🎉
```

---

**Last Updated**: 2026-01-10
**Environment**: Containerized (limited)
**Recommendation**: Build on local machine with Android NDK
**Status**: Prerequisites 75% met - NDK required for completion
