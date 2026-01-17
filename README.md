# Builder — Simple Mobile Orchestrator

**GitHub Actions Builder · Non-Root Smartphone Runtime**

Builder is a mobile orchestration system that provides a **Coolify/Render-like "build → deploy → run/manage"** experience on non-root Android smartphones.

> **Core invariant:**
> The phone is the **runtime + control UI**.
> GitHub Actions is the **builder**.
> The phone never builds from source.

---

## 📋 Overview

Builder allows you to:

- ✅ Connect to GitHub and trigger builds via GitHub Actions
- ✅ Install prebuilt **Packs** (WASM/workflow bundles) on your phone
- ✅ Run Packs in a sandboxed runtime with resource limits
- ✅ Manage instances (Start / Pause / Stop)
- ✅ View logs and basic health metrics
- ✅ Install deterministically with strict naming + `packs.index.json`

### What Builder IS

- A non-root smartphone orchestration app
- A local runtime for WASM and workflow packs
- A GitHub Actions integration tool
- A pack instance manager with lifecycle control

### What Builder IS NOT

- ❌ Docker
- ❌ Kubernetes
- ❌ A general PaaS with uptime guarantees
- ❌ A phone-side build system or CI runner

---

## 🏗️ Architecture

```
┌─────────────────┐         ┌──────────────────┐
│  GitHub Actions │────────▶│  GitHub Releases │
│   (Builder)     │         │   (Artifacts)    │
└─────────────────┘         └──────────────────┘
                                     │
                                     │ Download
                                     ▼
                            ┌─────────────────┐
                            │  Android Phone  │
                            │   (Runtime)     │
                            │                 │
                            │  ┌───────────┐  │
                            │  │  Packs    │  │
                            │  └───────────┘  │
                            │  ┌───────────┐  │
                            │  │ Instances │  │
                            │  └───────────┘  │
                            └─────────────────┘
```

### Module Structure

- **`app/`** - Main Android application
- **`core/`** - Core models and utilities (pack manifests, naming conventions)
- **`data/`** - Data layer (Room database, GitHub API, file storage)
- **`runtime/`** - WASM and Workflow execution engines
- **`domain/`** - Use cases and business logic
- **`ui/`** - Jetpack Compose UI components
- **`native/`** - Native code (Wasmtime JNI bridge)
- **`pack-templates/`** - Example pack templates

---

## 🚀 Getting Started

### ⚡ **IMPORTANT: Builds Happen on GitHub Actions (Not Locally!)**

**Builder uses GitHub Actions for all builds.** You do NOT need to build locally or install Android SDK/NDK on your machine.

### Building the App

#### ✅ **Recommended: GitHub Actions (Automated)**

This is the **official and recommended** way to build Builder:

1. **Push code to GitHub:**

```bash
git push origin your-branch-name
```

2. **GitHub Actions automatically builds** (triggers on push to `main`, `develop`, or `claude/**` branches):
   - ✅ Sets up Android SDK
   - ✅ Downloads all dependencies
   - ✅ Runs tests
   - ✅ Builds Debug APK
   - ✅ Builds Release APK
   - ✅ Runs Lint checks

3. **Download APK from GitHub Actions:**
   - Go to the [Actions tab](https://github.com/RachEma-ux/Builder/actions)
   - Select your workflow run
   - Download `builder-debug-apk` artifact (retained for 30 days)
   - Or `builder-release-apk` for production (retained for 90 days)

4. **Install on your Android device:**
   - Transfer APK to phone and open it, OR
   - Use `adb install app-debug.apk`

**See [BUILD_AND_DEPLOY.md](BUILD_AND_DEPLOY.md) for detailed workflow diagrams.**

---

#### 🔧 Optional: Local Development Build

**Only needed for:** Active development with Android Studio (code editing, debugging)

**Prerequisites:**
- Android Studio Hedgehog or later
- JDK 17+
- Android SDK API 34+
- Gradle 8.2+
- ⚠️ **Internet connection** (for dependency downloads)

**Steps:**

```bash
git clone https://github.com/RachEma-ux/Builder.git
cd Builder
./gradlew build
```

**Note:** Local builds may fail in restricted network environments. **Use GitHub Actions instead.**

---

## 📦 Creating Packs

Packs are the fundamental unit of deployment in Builder. See the [Pack Builder Guide](docs/builder-guide.md) for details.

### Quick Start: WASM Pack

1. Use the `pack-templates/wasm-hello` template
2. Write your WASM code (Rust recommended)
3. Create a `pack.json` manifest
4. Set up GitHub Actions workflow (see template)
5. Tag a release to publish

### Pack Structure

```
pack-<variant>-<target>-<version>.zip
  ├── pack.json              # Manifest with permissions, limits
  ├── main.wasm              # WASM binary (for type=wasm)
  └── assets/                # Optional assets
```

### Pack Naming Convention (Mandatory)

All packs **MUST** follow this naming:

```
pack-<variant>-<target>-<version>.zip
```

Examples:
- `pack-hello-android-arm64-v1.0.0.zip`
- `pack-workflow-test-android-universal-v0.1.0.zip`

See `Builder_Final.md` §5 for complete specification.

---

## 🔒 Install Modes

Builder has **two distinct install modes** that are impossible to confuse:

### DEV Mode (Workflow Artifacts)

- Source: **Branches or commits**
- Install from: **GitHub workflow artifacts only**
- Use case: Fast iteration, testing
- Artifacts are explicitly **ephemeral** (expire after 90 days)

### PROD Mode (Release Assets)

- Source: **Git tags** (e.g., `v1.2.3`)
- Install from: **GitHub Release assets only**
- Requires: `packs.index.json` and `checksums.sha256`
- Use case: Stable, auditable deployments

**These modes are enforced by the UI with tabs, badges, and banners. See `Builder_Final.md` Appendix A.**

---

## 🎯 Roadmap

**Last Updated**: 2026-01-17

### Phase 1: Core Infrastructure ✅ Complete
- [x] Android app scaffold
- [x] Core models (PackManifest, PackIndex, etc.)
- [x] WASM runtime structure
- [x] Workflow runtime structure
- [x] Build system configuration

### Phase 2: GitHub Integration ✅ Complete
- [x] OAuth authorization code flow with PKCE
- [x] Repository listing
- [x] Branch/tag/release browsing
- [x] Workflow dispatch
- [x] Artifact downloads (Dev mode)
- [x] Release asset downloads (Prod mode)
- [x] Encrypted token storage

### Phase 3: Pack Management ✅ Complete
- [x] Local pack installation pipeline
- [x] Dev vs Prod mode enforcement (separate tabs)
- [x] Checksum verification (SHA-256, mandatory for Prod)
- [x] Pack lifecycle management
- [x] Zip slip vulnerability protection
- [x] Naming convention validation

### Phase 4: Runtimes ⚠️ Partial
- [ ] WASM execution (Wasmtime integration) — **Blocked: needs native library build**
- [x] Workflow execution engine
- [x] Permission enforcement
- [x] Progress tracking and cancellation

### Phase 5: UI/UX ✅ ~90% Complete
- [x] IBM-style sidebar navigation
- [x] GitHub Packs screen (Dev/Prod tabs)
- [x] Production tab: auto-load checksums, Install button
- [x] Dev tab: branch selection, workflow artifacts
- [x] Instance management screen
- [x] Logs viewer with filtering/search
- [x] Health monitoring (CPU, memory, network)
- [ ] Secrets management UI

### Phase 6: Production Hardening ⚠️ ~60% Complete
- [ ] Security audit
- [ ] Performance optimization
- [x] Error handling (user-friendly messages)
- [x] Documentation (comprehensive)
- [x] CI/CD pipeline (GitHub Actions)
- [ ] Persistent KV store (currently in-memory)

---

### 🚧 Remaining Work

| Task | Priority | Status |
|------|----------|--------|
| Build Wasmtime for Android | High | Blocked (needs Rust toolchain) |
| Secrets management UI | Medium | Not started |
| Persistent KV store | Medium | Not started |
| Security audit | Medium | Not started |
| UI polish (animations) | Low | Not started |
| Increase test coverage | Low | 15% current |

### ✅ Recent Fixes (2026-01-17)
- Fixed Production tab 404 error when tag has no release
- Auto-load checksums when selecting a release
- Install button appears directly after checksums load
- Better error messages for missing releases

---

## 📚 Documentation

- **[Builder_Final.md](Builder_Final.md)** - Complete specification
- **[Implementation Plan](docs/IMPLEMENTATION_PLAN.md)** - Generated from spec analysis
- **[Pack Builder Guide](docs/builder-guide.md)** - How to create packs (TODO)
- **[User Guide](docs/user-guide.md)** - How to use the app (TODO)

---

## 🛠️ Development

### Project Structure

```
Builder/
├── app/                    # Main Android app
├── core/                   # Pure Kotlin business logic
│   ├── model/             # Data models
│   ├── repository/        # Repository interfaces
│   └── util/              # Utilities
├── data/                  # Data layer implementations
│   ├── local/            # Room DB, file storage
│   └── remote/           # GitHub API client
├── runtime/               # Execution engines
│   ├── wasm/             # WASM runtime (Wasmtime)
│   └── workflow/         # Workflow engine
├── domain/                # Use cases
├── ui/                    # Compose UI
├── native/                # C/JNI code
└── pack-templates/        # Example packs
```

### Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt (Dagger)
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **WASM Runtime**: Wasmtime (via JNI)
- **Build**: Gradle

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lintDebug
```

---

## 🤝 Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) (TODO).

### Development Workflow

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the Apache License 2.0 - see [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- Wasmtime by the Bytecode Alliance
- GitHub Actions for CI/CD
- Android Jetpack for modern Android development
- The open source community

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/RachEma-ux/Builder/issues)
- **Discussions**: [GitHub Discussions](https://github.com/RachEma-ux/Builder/discussions)

---

**Built with ❤️ for non-root smartphone orchestration**
