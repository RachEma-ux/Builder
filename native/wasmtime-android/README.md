# Wasmtime JNI Bridge for Android

This directory contains the JNI bridge that connects Kotlin/Java code to the Wasmtime WebAssembly runtime.

## Directory Structure

```
wasmtime-android/
├── CMakeLists.txt          # CMake build configuration
├── src/
│   └── wasmtime_jni.c      # JNI bridge implementation
├── include/                # Wasmtime C API headers (after build)
│   ├── wasmtime.h
│   └── wasm.h
├── libs/                   # Prebuilt Wasmtime libraries (after build)
│   ├── arm64-v8a/
│   │   └── libwasmtime.so
│   └── x86_64/
│       └── libwasmtime.so
└── README.md               # This file
```

## Building Wasmtime

### Automated Build

Use the provided script:

```bash
cd /path/to/Builder
./scripts/build-wasmtime.sh v15.0.0
```

This will:
1. Clone Wasmtime from GitHub
2. Build for Android ARM64 and x86_64
3. Copy libraries and headers to this directory
4. Strip debug symbols
5. Create version file

### Manual Build

See [docs/WASMTIME_BUILD_GUIDE.md](../../docs/WASMTIME_BUILD_GUIDE.md) for detailed instructions.

## JNI Bridge API

The JNI bridge (`wasmtime_jni.c`) exposes these native functions to `WasmRuntime.kt`:

### Engine Management

```c
JNIEXPORT jlong JNICALL
Java_com_builder_runtime_wasm_WasmRuntime_wasmtimeCreateEngine(
    JNIEnv *env, jobject thiz
);
```
Creates a Wasmtime engine with default configuration.

**Returns**: Engine handle (pointer)

### Module Loading

```c
JNIEXPORT jlong JNICALL
Java_com_builder_runtime_wasm_WasmRuntime_wasmtimeLoadModule(
    JNIEnv *env, jobject thiz,
    jlong engineHandle,
    jbyteArray wasmBytes
);
```
Loads a WASM module from byte array.

**Parameters**:
- `engineHandle`: Engine handle from `wasmtimeCreateEngine`
- `wasmBytes`: WASM module bytes

**Returns**: Module handle (pointer)

### Instance Creation

```c
JNIEXPORT jlong JNICALL
Java_com_builder_runtime_wasm_WasmRuntime_wasmtimeCreateInstance(
    JNIEnv *env, jobject thiz,
    jlong engineHandle,
    jlong moduleHandle
);
```
Creates an instance of a loaded module.

**Parameters**:
- `engineHandle`: Engine handle
- `moduleHandle`: Module handle from `wasmtimeLoadModule`

**Returns**: Instance handle (pointer)

### Function Execution

```c
JNIEXPORT jint JNICALL
Java_com_builder_runtime_wasm_WasmRuntime_wasmtimeCallFunction(
    JNIEnv *env, jobject thiz,
    jlong instanceHandle,
    jstring functionName,
    jintArray args,
    jintArray results
);
```
Calls an exported WASM function.

**Parameters**:
- `instanceHandle`: Instance handle
- `functionName`: Name of exported function
- `args`: Input arguments (i32 array)
- `results`: Output array for results

**Returns**: 0 on success, -1 on error

### Cleanup

```c
JNIEXPORT void JNICALL
Java_com_builder_runtime_wasm_WasmRuntime_wasmtimeCleanup(
    JNIEnv *env, jobject thiz,
    jlong engineHandle,
    jlong moduleHandle,
    jlong instanceHandle
);
```
Frees all allocated resources.

**Parameters**: Handles from previous calls

## Usage from Kotlin

```kotlin
val wasmRuntime = WasmRuntime()

// Load WASM module
val moduleBytes = File("app.wasm").readBytes()

// Execute function
val result = wasmRuntime.executeFunction(
    moduleBytes = moduleBytes,
    functionName = "add",
    args = intArrayOf(5, 7)
)

println("Result: ${result.getOrNull()}") // Output: [12]
```

## Current Status

### ✅ Implemented
- JNI bridge structure
- Function signatures matching Wasmtime C API
- Kotlin wrapper (`WasmRuntime.kt`)
- CMake build configuration
- Error handling framework

### ⚠️ Stub Implementation
- All native functions return placeholder values
- No actual Wasmtime calls (library not linked)
- Will fail at runtime if called

### 🔨 Required for Production
1. **Build Wasmtime**: Run `./scripts/build-wasmtime.sh`
2. **Implement JNI functions**: Replace stubs in `wasmtime_jni.c`
3. **Memory management**: Implement proper cleanup
4. **Error handling**: Map Wasmtime errors to Java exceptions
5. **Testing**: Create comprehensive test suite

## Implementation Checklist

When implementing the JNI bridge, ensure:

- [ ] Proper JNI error checking (`env->ExceptionCheck()`)
- [ ] Memory management (no leaks)
- [ ] UTF-8 string handling for function names
- [ ] Array bounds checking
- [ ] Thread safety (if multi-threaded)
- [ ] Wasmtime error propagation to Java
- [ ] Logging via Android `__android_log_print`

## Security Considerations

### Sandboxing

Wasmtime provides sandboxing, but the JNI bridge must:
- Validate all inputs from Java side
- Prevent buffer overflows
- Limit memory allocation
- Enforce timeouts on execution

### Permission Checking

Before executing WASM:
```kotlin
// Check pack permissions
val allowed = permissionEnforcer.checkPermissions(
    packManifest = pack.manifest,
    requestedOperation = "execute_wasm"
)

if (!allowed) {
    throw SecurityException("Pack does not have execute permission")
}
```

## Performance Optimization

### AOT Compilation

For production, use Ahead-of-Time compilation:

```c
// In wasmtime_jni.c
wasmtime_module_serialize(module, &wasm_aot);
// Save to file, load later for faster startup
```

### Memory Limits

Set per-pack memory limits in `WasiConfig`:

```kotlin
val config = WasiConfig.Builder()
    .maxMemoryMb(pack.manifest.limits.memoryMb)
    .build()
```

### Caching

Cache compiled modules:
```kotlin
val cacheDir = context.cacheDir.resolve("wasm-cache")
wasmRuntime.setCacheDir(cacheDir)
```

## Debugging

### Enable Logging

```c
#define LOG_TAG "WasmtimeJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

LOGD("Calling function: %s", function_name);
```

### Check Library Loading

```bash
adb shell
run-as com.builder
ls -l lib/
# Should show libwasmtime.so and libwasmtime_jni.so
```

### Test Locally

```bash
adb logcat | grep -E "WasmtimeJNI|Wasmtime"
```

## Troubleshooting

### Build Errors

**Error**: `undefined reference to wasmtime_*`
- **Fix**: Run `./scripts/build-wasmtime.sh` to build libraries
- **Check**: `ls -l native/wasmtime-android/libs/arm64-v8a/`

### Runtime Errors

**Error**: `java.lang.UnsatisfiedLinkError`
- **Cause**: Native library not found or not loaded
- **Fix**: Check APK contains both `.so` files
- **Verify**: `unzip -l app-debug.apk | grep wasmtime`

**Error**: `SIGSEGV` in native code
- **Cause**: NULL pointer dereference, memory corruption
- **Fix**: Use AddressSanitizer for debugging
- **Build**: Add `-fsanitize=address` to CMake

## Resources

- **Wasmtime C API**: https://docs.wasmtime.dev/c-api/
- **JNI Specification**: https://docs.oracle.com/javase/8/docs/technotes/guides/jni/
- **Android NDK Guide**: https://developer.android.com/ndk/guides

## Version Information

- **Wasmtime Version**: See `WASMTIME_VERSION` file after build
- **JNI Version**: JNI_VERSION_1_6
- **Minimum Android API**: 26 (Android 8.0 Oreo)

---

**Last Updated**: 2026-01-10
**Status**: Stub implementation, requires Wasmtime compilation
