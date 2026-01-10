# Wasmtime Build Guide for Builder

This guide explains how to build Wasmtime for Android and integrate it with the Builder project.

## Overview

Builder uses **Wasmtime** as its WebAssembly runtime. Wasmtime must be compiled as a native library for Android targets (ARM64 and x86_64) and linked via JNI.

## Prerequisites

### Required Tools

- **Rust toolchain** (1.70+)
  ```bash
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
  source $HOME/.cargo/env
  ```

- **Android NDK** r25+
  - Download from: https://developer.android.com/ndk/downloads
  - Extract to: `~/Android/Sdk/ndk/25.2.9519653` (or update paths)

- **Cargo NDK** for cross-compilation
  ```bash
  cargo install cargo-ndk
  ```

- **Android Rust Targets**
  ```bash
  rustup target add aarch64-linux-android
  rustup target add x86_64-linux-android
  ```

### Environment Variables

Add to your `~/.bashrc` or `~/.zshrc`:

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin
```

## Building Wasmtime

### Step 1: Clone Wasmtime

```bash
cd /tmp
git clone https://github.com/bytecodealliance/wasmtime.git
cd wasmtime
git checkout v15.0.0  # Use stable release
```

### Step 2: Build for Android ARM64

```bash
cargo ndk \
  --target aarch64-linux-android \
  --platform 26 \
  build \
  --release \
  --manifest-path crates/c-api/Cargo.toml
```

Output: `target/aarch64-linux-android/release/libwasmtime.so`

### Step 3: Build for Android x86_64

```bash
cargo ndk \
  --target x86_64-linux-android \
  --platform 26 \
  build \
  --release \
  --manifest-path crates/c-api/Cargo.toml
```

Output: `target/x86_64-linux-android/release/libwasmtime.so`

### Step 4: Copy to Builder Project

```bash
# Create directories
mkdir -p Builder/native/wasmtime-android/libs/arm64-v8a
mkdir -p Builder/native/wasmtime-android/libs/x86_64

# Copy libraries
cp target/aarch64-linux-android/release/libwasmtime.so \
   Builder/native/wasmtime-android/libs/arm64-v8a/

cp target/x86_64-linux-android/release/libwasmtime.so \
   Builder/native/wasmtime-android/libs/x86_64/

# Copy headers
cp crates/c-api/include/wasmtime.h \
   Builder/native/wasmtime-android/include/

cp crates/c-api/include/wasm.h \
   Builder/native/wasmtime-android/include/
```

## Integration with Builder

### CMakeLists.txt Configuration

The `Builder/native/wasmtime-android/CMakeLists.txt` file is already configured to:
1. Import the prebuilt `libwasmtime.so` for each ABI
2. Link it with the JNI bridge (`libwasmtime_jni.so`)
3. Include Wasmtime headers

### JNI Bridge

The JNI bridge (`wasmtime_jni.c`) provides Java-callable functions that wrap Wasmtime C API.

**Key functions**:
- `wasmtimeCreateEngine()` - Initialize Wasmtime engine
- `wasmtimeLoadModule()` - Load WASM module from bytes
- `wasmtimeCreateInstance()` - Instantiate WASM module
- `wasmtimeCallFunction()` - Call exported WASM function
- `wasmtimeCleanup()` - Free resources

### Building Builder with Wasmtime

Once Wasmtime libraries are in place:

```bash
cd Builder
./gradlew assembleDebug
```

The build will:
1. Compile `wasmtime_jni.c` with CMake
2. Link against `libwasmtime.so`
3. Package both libraries into the APK
4. Generate JNI bindings in `WasmRuntime.kt`

## Testing

### Unit Test WASM Module

Create a simple test WASM module:

**test.wat** (WebAssembly Text Format):
```wasm
(module
  (func $add (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add)
  (export "add" (func $add))
)
```

Compile with `wat2wasm`:
```bash
wat2wasm test.wat -o test.wasm
```

### Test in Builder

```kotlin
val wasmRuntime = WasmRuntime()
val moduleBytes = File("test.wasm").readBytes()

val result = wasmRuntime.executeFunction(
    moduleBytes = moduleBytes,
    functionName = "add",
    args = intArrayOf(5, 7)
)
// result should be 12
```

## Troubleshooting

### Build Errors

**Error**: `error: linking with 'cc' failed`
- **Fix**: Ensure NDK is installed and `ANDROID_NDK_HOME` is set

**Error**: `cannot find -lwasmtime`
- **Fix**: Run Wasmtime build steps first, copy `.so` files to correct locations

### Runtime Errors

**Error**: `java.lang.UnsatisfiedLinkError: dlopen failed: library "libwasmtime.so" not found`
- **Fix**: Check that both `libwasmtime.so` and `libwasmtime_jni.so` are in APK
- Verify: `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep wasmtime`

**Error**: `SIGSEGV` or crashes
- **Fix**: Ensure ABI matches (ARM64 device needs arm64-v8a library)
- Check memory limits in pack manifest

### Performance Issues

**Problem**: Slow WASM execution
- **Solution**: Ensure building with `--release` flag
- **Solution**: Enable Wasmtime optimizations in `WasiConfig`
- **Solution**: Use AOT compilation for production builds

## Alternative: Pre-built Binaries

If building from source is not feasible, you can:

1. **Use GitHub Actions** to build Wasmtime
   - Fork Wasmtime repo
   - Add Android CI workflow
   - Download artifacts

2. **Use Docker** for reproducible builds
   ```bash
   docker run --rm -v $(pwd):/workspace \
     -w /workspace/wasmtime \
     rust:latest \
     bash -c "cargo ndk build ..."
   ```

3. **Community Builds** (verify checksums!)
   - Check if Bytecode Alliance provides Android builds
   - Use at your own risk

## Production Considerations

### Security

- **Verify Wasmtime version**: Use official releases only
- **Check signatures**: Verify git tags with GPG
- **Audit dependencies**: Review Wasmtime's Cargo.lock

### Size Optimization

Wasmtime library is large (~10-15 MB per ABI). To reduce APK size:

1. **Enable ABI splits** in `app/build.gradle.kts`:
   ```kotlin
   android {
       splits {
           abi {
               isEnable = true
               reset()
               include("arm64-v8a", "x86_64")
               isUniversalApk = false
           }
       }
   }
   ```

2. **Strip debug symbols**:
   ```bash
   $ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip \
     libwasmtime.so
   ```

3. **Use ProGuard/R8**: Already enabled for release builds

### Updates

To update Wasmtime:

1. Check release notes: https://github.com/bytecodealliance/wasmtime/releases
2. Test thoroughly with existing packs
3. Update version in documentation
4. Rebuild and re-test JNI bridge

## Resources

- **Wasmtime Documentation**: https://docs.wasmtime.dev/
- **Wasmtime C API Guide**: https://docs.wasmtime.dev/c-api/
- **Android NDK Guide**: https://developer.android.com/ndk/guides
- **Cargo NDK**: https://github.com/bbqsrc/cargo-ndk
- **WebAssembly Specification**: https://webassembly.github.io/spec/

## Support

For issues with:
- **Wasmtime build**: Open issue at https://github.com/bytecodealliance/wasmtime/issues
- **Builder integration**: Open issue at https://github.com/RachEma-ux/Builder/issues
- **Android NDK**: Check https://developer.android.com/ndk/guides/troubleshooting

---

**Last Updated**: 2026-01-10
**Wasmtime Version**: 15.0.0
**NDK Version**: r25+
**Minimum Android API**: 26 (Android 8.0)
