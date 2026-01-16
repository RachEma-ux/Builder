# Developer Quick Start Guide

Get Builder up and running in **30 minutes** (or less)!

---

## ⚡ **CHOOSE YOUR PATH:**

### 🎯 **Path A: Just Want to Build & Test?** → Use GitHub Actions (5 minutes)

**✅ Recommended for:** Testing, deploying, contributing code

**You DON'T need:**
- ❌ Android Studio installed
- ❌ Android SDK/NDK setup
- ❌ Local build environment
- ❌ Gradle configuration

**Steps:**
1. Fork the repo on GitHub
2. Make changes in any text editor
3. Push to GitHub
4. Download APK from GitHub Actions
5. Install on device

**👉 See [BUILD_AND_DEPLOY.md](BUILD_AND_DEPLOY.md) for complete instructions.**

---

### 🛠️ **Path B: Active Android Development?** → Android Studio Setup (30 minutes)

**Only needed if:** You're actively developing UI, debugging, or need IDE features

**Continue reading below for local development setup...**

---

## 📋 Prerequisites

### Required
- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Git**
- **Android SDK** with:
  - Android 14 (API 34)
  - Android 8.0 (API 26) minimum
- **10 GB** free disk space

### Optional (for WASM runtime)
- **Rust** 1.70+
- **Android NDK** r25+
- **cargo-ndk**

## 🚀 Quick Setup (5 minutes)

### 1. Clone the Repository

```bash
git clone https://github.com/RachEma-ux/Builder.git
cd Builder
```

### 2. Configure GitHub OAuth

**Create OAuth App**:
1. Go to https://github.com/settings/applications/new
2. Fill in:
   - **Name**: `Builder Dev - [Your Name]`
   - **Homepage**: `https://github.com/RachEma-ux/Builder`
   - **Callback**: `builder://oauth/callback`
3. Click **Register**
4. Copy the **Client ID**

**Update Code**:
```bash
# Edit this file
nano data/remote/github/GitHubOAuthService.kt

# Find this line (line 28):
const val CLIENT_ID = "Ov23liPLACEHOLDER_UPDATE_ME"

# Replace with your Client ID:
const val CLIENT_ID = "Ov23liYourActualClientId"
```

**Or use BuildConfig** (recommended):
```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"Ov23liYourClientId\"")
    }
}

// GitHubOAuthService.kt
const val CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
```

See [docs/GITHUB_OAUTH_SETUP.md](docs/GITHUB_OAUTH_SETUP.md) for details.

### 3. Open in Android Studio

```bash
# Launch Android Studio
android-studio Builder/
```

Or:
1. Open Android Studio
2. **File → Open**
3. Select `Builder` directory
4. Wait for Gradle sync (2-3 minutes)

### 4. Build & Run

**Connect Device/Emulator**:
- Physical device: Enable USB debugging
- Emulator: Create AVD (Android 8.0+, ARM64 recommended)

**Build**:
```bash
./gradlew assembleDebug
```

**Run**:
- Click **Run** (Shift+F10)
- Or: `./gradlew installDebug`

**First Launch**:
- App opens to GitHub Packs screen
- Tap "Sign in with GitHub"
- Complete device flow
- Browse your repos!

## 🎯 What Works Out of the Box

✅ **Core Infrastructure** (100%)
- Multi-module architecture
- Dependency injection (Hilt)
- Room database
- Repository pattern

✅ **GitHub Integration** (95%)
- OAuth device flow
- Repository browsing
- Branch/tag/release listing
- Workflow dispatch
- Artifact download
- ⚠️ Needs: Your OAuth Client ID

✅ **Pack Management** (100%)
- Dev mode install (from artifacts)
- Prod mode install (from releases with index)
- Naming convention enforcement
- Checksum verification
- Manifest validation

✅ **Logs & Monitoring** (100%)
- Real-time log streaming
- Filtering by level/source
- Search functionality
- Health metrics (CPU, memory, network)
- Uptime tracking

✅ **Workflow Engine** (100%)
- HTTP requests
- KV store (in-memory)
- Logging
- Sleep/delay
- Step-by-step execution
- Progress tracking

⚠️ **WASM Runtime** (40%)
- JNI bridge structure ready
- Native stub implementation
- ❌ Needs: Wasmtime compilation

## ⚙️ Building Wasmtime (Optional, 2-3 hours)

**Only needed if you want to run WASM packs.**

### Prerequisites

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env

# Install cargo-ndk
cargo install cargo-ndk

# Add Android targets
rustup target add aarch64-linux-android x86_64-linux-android
```

### Set Environment

```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.2.9519653
export PATH=$PATH:$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin
```

### Build Wasmtime

```bash
cd Builder
./scripts/build-wasmtime.sh v15.0.0
```

This will:
1. Clone Wasmtime
2. Build for ARM64 and x86_64
3. Copy libraries to `native/wasmtime-android/libs/`
4. Strip debug symbols
5. Generate version file

**Expected output**:
```
✅ Wasmtime built successfully!
   - arm64-v8a: 12.3 MB
   - x86_64: 13.1 MB
   Total: 25.4 MB
```

### Rebuild Builder

```bash
./gradlew clean assembleDebug
```

The build will link against Wasmtime and enable full WASM support.

**See**: [docs/WASMTIME_BUILD_GUIDE.md](docs/WASMTIME_BUILD_GUIDE.md) for troubleshooting.

## 🧪 Running Tests

### Unit Tests

```bash
./gradlew test
```

**Current coverage**: ~15%

**Test files**:
- `domain/src/test/**/*Test.kt` (Use cases)
- `data/src/test/**/*Test.kt` (PackStorage)

### Integration Tests

```bash
./gradlew connectedAndroidTest
```

**Test files**:
- `data/src/androidTest/**/*Test.kt` (Room DAOs)

### Lint

```bash
./gradlew lint
```

## 📁 Project Structure

```
Builder/
├── app/                    # Android app module
│   ├── src/main/
│   │   ├── java/com/builder/
│   │   │   ├── BuilderApplication.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── di/         # Hilt modules
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── core/                   # Pure Kotlin core
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── util/               # Utilities
│
├── data/                   # Data layer
│   ├── local/              # Room DB, file storage
│   ├── remote/             # GitHub API client
│   └── repository/         # Repository implementations
│
├── domain/                 # Business logic
│   └── usecases/           # Use case classes
│
├── runtime/                # Execution engines
│   ├── wasm/               # WASM runtime
│   ├── workflow/           # Workflow engine
│   ├── LogCollector.kt     # Log collection
│   └── HealthMonitor.kt    # Health metrics
│
├── ui/                     # Jetpack Compose UI
│   ├── screens/            # Screen composables
│   ├── viewmodel/          # ViewModels
│   └── theme/              # Material3 theme
│
├── native/                 # Native code
│   └── wasmtime-android/   # JNI bridge
│       ├── src/wasmtime_jni.c
│       ├── CMakeLists.txt
│       └── libs/           # Wasmtime binaries (after build)
│
├── pack-templates/         # Example packs
│   └── wasm-hello/         # Rust WASM template
│
├── docs/                   # Documentation
│   ├── WASMTIME_BUILD_GUIDE.md
│   └── GITHUB_OAUTH_SETUP.md
│
├── scripts/                # Build scripts
│   └── build-wasmtime.sh
│
└── build.gradle.kts        # Root build file
```

## 🔍 Key Files to Know

### Android App
- **`app/src/main/java/com/builder/MainActivity.kt`** - Entry point
- **`app/src/main/java/com/builder/di/*.kt`** - Dependency injection
- **`app/src/main/AndroidManifest.xml`** - Permissions, activities

### Core Models
- **`core/model/PackManifest.kt`** - Pack manifest (pack.json)
- **`core/model/Instance.kt`** - Pack instance lifecycle
- **`core/model/Workflow.kt`** - Workflow definition

### Data Layer
- **`data/local/db/BuilderDatabase.kt`** - Room database
- **`data/repository/*RepositoryImpl.kt`** - Data access
- **`data/remote/github/GitHubOAuthManager.kt`** - OAuth flow

### Runtime
- **`runtime/wasm/WasmRuntime.kt`** - WASM execution
- **`runtime/workflow/WorkflowEngine.kt`** - Workflow execution
- **`runtime/InstanceManager.kt`** - Instance lifecycle

### UI
- **`ui/screens/GitHubPacksScreen.kt`** - GitHub integration UI
- **`ui/screens/InstancesScreen.kt`** - Instance management
- **`ui/screens/LogsScreen.kt`** - Log viewer
- **`ui/viewmodel/*.kt`** - UI state management

## 🐛 Debugging

### Enable Verbose Logging

```kotlin
// BuilderApplication.kt
override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
        Timber.d("Builder started in DEBUG mode")
    }
}
```

### View Logs

```bash
# Filter by Builder
adb logcat | grep Builder

# Filter by specific component
adb logcat | grep -E "WasmRuntime|WorkflowEngine|GitHubOAuth"

# View Room queries
adb logcat | grep RoomDatabase
```

### Inspect Database

```bash
adb shell
run-as com.builder
cd databases
sqlite3 builder.db

# Query tables
.tables
SELECT * FROM packs;
SELECT * FROM instances;
SELECT * FROM logs LIMIT 10;
```

### Check Installed Packs

```bash
adb shell
run-as com.builder
ls -la files/packs/
```

## 🚧 Known Limitations

1. **WASM Runtime**: Stub implementation until Wasmtime is built
2. **KV Store**: In-memory only (not persistent)
3. **Event Bus**: Not implemented (events logged only)
4. **Network Enforcement**: Allow-list not enforced
5. **Resource Limits**: Not enforced at runtime
6. **Workflow Cancellation**: Not fully implemented
7. **UI Polish**: Basic Material3, needs animations

## 📚 Documentation

- **[README.md](README.md)** - Project overview
- **[Builder_Final.md](Builder_Final.md)** - Full specification
- **[REPOSITORY_ANALYSIS.md](REPOSITORY_ANALYSIS.md)** - Code analysis
- **[PROJECT_STATUS_V2.md](PROJECT_STATUS_V2.md)** - Current status
- **[docs/WASMTIME_BUILD_GUIDE.md](docs/WASMTIME_BUILD_GUIDE.md)** - Wasmtime build
- **[docs/GITHUB_OAUTH_SETUP.md](docs/GITHUB_OAUTH_SETUP.md)** - OAuth setup
- **[native/wasmtime-android/README.md](native/wasmtime-android/README.md)** - JNI bridge

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** changes: `git commit -m 'Add amazing feature'`
4. **Push** to branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

**Before submitting**:
- [ ] Code builds without errors
- [ ] Tests pass (`./gradlew test`)
- [ ] Lint passes (`./gradlew lint`)
- [ ] Code follows Kotlin style guide
- [ ] Documentation updated

## ❓ FAQ

### Q: Build fails with "Failed to resolve: androidx.compose..."

**A**: Update Gradle plugin:
```kotlin
// build.gradle.kts
classpath("com.android.tools.build:gradle:8.2.0")
```

### Q: "Gradle sync failed: Could not find method compile()"

**A**: Use `implementation` instead of `compile`:
```kotlin
dependencies {
    implementation("...")  // ✅
    compile("...")         // ❌ Deprecated
}
```

### Q: "Execution failed for task ':app:kaptDebugKotlin'"

**A**: Clean and rebuild:
```bash
./gradlew clean build
```

### Q: "UnsatisfiedLinkError: dlopen failed: library 'libwasmtime.so' not found"

**A**: Build Wasmtime (see section above) or disable WASM packs for now.

### Q: GitHub OAuth returns "invalid_client"

**A**: Check:
1. Client ID is correct in `GitHubOAuthService.kt`
2. Callback URL matches: `builder://oauth/callback`
3. OAuth app is not suspended

### Q: App crashes on startup

**A**: Check logs:
```bash
adb logcat | grep AndroidRuntime
```

Common causes:
- Hilt dependency injection error
- Database migration failure
- Missing permission

## 🎓 Learning Resources

### Kotlin & Android
- **Kotlin Docs**: https://kotlinlang.org/docs/home.html
- **Android Developers**: https://developer.android.com/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose

### Architecture
- **Clean Architecture**: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- **MVVM Pattern**: https://developer.android.com/topic/architecture
- **Repository Pattern**: https://developer.android.com/codelabs/android-room-with-a-view

### Libraries Used
- **Hilt**: https://dagger.dev/hilt/
- **Room**: https://developer.android.com/training/data-storage/room
- **Retrofit**: https://square.github.io/retrofit/
- **OkHttp**: https://square.github.io/okhttp/
- **Timber**: https://github.com/JakeWharton/timber

### WebAssembly
- **Wasmtime**: https://docs.wasmtime.dev/
- **WASI**: https://wasi.dev/
- **Rust Book**: https://doc.rust-lang.org/book/

## 🆘 Getting Help

1. **Check Documentation**: See `docs/` folder
2. **Search Issues**: https://github.com/RachEma-ux/Builder/issues
3. **Ask Question**: Open a new issue with `question` label
4. **Join Discussions**: https://github.com/RachEma-ux/Builder/discussions

## 📈 Next Steps

After getting Builder running:

1. **Explore the UI**: Try browsing GitHub repos, signing in
2. **Install a Pack**: Use Dev mode to install from a workflow artifact
3. **View Logs**: Check the Logs screen for real-time output
4. **Create a Pack**: Use `pack-templates/wasm-hello/` as starting point
5. **Contribute**: Pick an issue labeled `good-first-issue`

## 🎉 Success!

If you've made it this far, you should have:
- ✅ Builder building successfully
- ✅ GitHub OAuth configured
- ✅ App running on device/emulator
- ✅ Understanding of project structure

**Welcome to the Builder project!** 🚀

---

**Last Updated**: 2026-01-10
**Version**: 0.1.0-alpha
**Status**: 80% Production-Ready
