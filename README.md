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

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17+
- Android SDK API 34+
- Android NDK r25+ (for native builds)
- Gradle 8.2+

### Building the App

1. **Clone the repository:**

```bash
git clone https://github.com/RachEma-ux/Builder.git
cd Builder
```

2. **Open in Android Studio:**

Open the project in Android Studio and let it sync Gradle dependencies.

3. **Build the project:**

```bash
./gradlew build
```

4. **Run on device/emulator:**

```bash
./gradlew installDebug
```

Or use Android Studio's Run button.

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

### Phase 1: Core Infrastructure ✅ (Scaffolding Complete)
- [x] Android app scaffold
- [x] Core models (PackManifest, PackIndex, etc.)
- [x] WASM runtime structure
- [x] Workflow runtime structure
- [x] Build system configuration

### Phase 2: GitHub Integration (In Progress)
- [ ] OAuth device flow
- [ ] Repository listing
- [ ] Workflow dispatch
- [ ] Artifact/Release downloads

### Phase 3: Pack Management
- [ ] Local pack installation
- [ ] Dev vs Prod mode enforcement
- [ ] Checksum verification
- [ ] Pack lifecycle management

### Phase 4: Runtimes
- [ ] WASM execution (Wasmtime integration)
- [ ] Workflow execution
- [ ] Permission enforcement

### Phase 5: UI/UX
- [ ] IBM-style sidebar navigation
- [ ] GitHub Packs screen (Dev/Prod tabs)
- [ ] Instance management screen
- [ ] Logs viewer
- [ ] Secrets management

### Phase 6: Production Hardening
- [ ] Security audit
- [ ] Performance optimization
- [ ] Error handling
- [ ] Documentation

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
