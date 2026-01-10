# Builder - One-Tap Installation Guide

Quick installation guide for Builder Android app using Termux.

---

## 🚀 Quick Start (One Command)

```bash
cd ~/Builder && ./install.sh
```

That's it! The script will:
1. ✅ Check and install prerequisites (Java, ADB, Git)
2. ✅ Build the Builder app (~5-10 minutes first time)
3. ✅ Install the APK on your device
4. ✅ Launch the app

---

## 📋 Prerequisites

Before running the installer, make sure you have:

### 1. Termux Installed
- Download from: [F-Droid](https://f-droid.org/packages/com.termux/)
- **NOT** from Google Play (outdated version)

### 2. Developer Options Enabled
On your Android device:
```
Settings → About Phone → Tap "Build Number" 7 times
```

### 3. USB Debugging Enabled
```
Settings → Developer Options → Enable "USB Debugging"
```

### 4. Clone the Repository (if not done)
```bash
pkg install git
git clone https://github.com/RachEma-ux/Builder.git
cd Builder
```

---

## 🎯 Installation Methods

### Method 1: Full Install (Build + Install)
**Recommended for development**

```bash
./install.sh
```

This builds from source and installs on your device.

**First time**: ~10 minutes (downloads dependencies)
**Subsequent**: ~2-3 minutes (incremental build)

### Method 2: Build Only
**If you just want the APK file**

```bash
./install.sh --build-only
```

APK will be saved to:
- `app/build/outputs/apk/debug/app-debug.apk`
- `/sdcard/Download/builder-debug.apk` (for sharing)

### Method 3: Install Only
**If you already have a built APK**

```bash
./install.sh --install-only
```

Installs the previously built APK.

---

## 📱 What Gets Installed

**App Name**: Builder
**Package**: `com.builder`
**Size**: ~15-20 MB
**Permissions Required**:
- Internet (for GitHub API)
- Storage (for pack installation)
- Foreground Service (for running instances)

---

## 🔧 Troubleshooting

### Build Failed - Out of Memory

**Problem**: Gradle runs out of memory

**Solution**: Increase heap size
```bash
# Edit gradle.properties
nano gradle.properties

# Change this line:
org.gradle.jvmargs=-Xmx2048m

# To:
org.gradle.jvmargs=-Xmx4096m
```

### ADB Not Found

**Problem**: `adb: command not found`

**Solution**: Install android-tools
```bash
pkg install android-tools
```

### Device Not Detected

**Problem**: `adb devices` shows empty

**Solution**:
1. Enable USB debugging on your device
2. Connect via USB
3. Accept the USB debugging prompt
4. Run: `adb devices`

### Build Takes Too Long

**Problem**: First build is very slow

**Solution**: This is normal!
- First build: ~10 minutes (downloads all dependencies)
- Second build: ~2 minutes (only compiles changes)
- Use `--install-only` if you just want to reinstall

### Permission Denied

**Problem**: `Permission denied` when running script

**Solution**: Make it executable
```bash
chmod +x install.sh
./install.sh
```

---

## 🎮 Using the App

### First Launch

1. **Open Builder** from your app drawer
2. **Grant permissions** when prompted
3. **Navigate** to "GitHub Packs" tab

### GitHub OAuth Setup

1. Tap **"Sign in with GitHub"**
2. You'll see a **device code** (e.g., `1234-ABCD`)
3. Open **https://github.com/login/device** in your browser
4. Enter the code
5. Authorize "Builder - Mobile Orchestration"
6. Return to the app
7. ✅ You'll see your repositories!

### Features

- **Browse GitHub repos** - All your repositories
- **Install packs** - From releases, branches, or workflow artifacts
- **Run instances** - Execute workflow steps
- **Monitor health** - CPU, memory, network stats
- **View logs** - Real-time log streaming
- **Manage instances** - Start, stop, view details

---

## 🔄 Updating the App

### Pull Latest Changes

```bash
cd ~/Builder
git pull
./install.sh
```

### Rebuild from Scratch

```bash
cd ~/Builder
./gradlew clean
./install.sh
```

---

## 🗑️ Uninstalling

### Remove App from Device

```bash
adb uninstall com.builder
```

Or from Settings:
```
Settings → Apps → Builder → Uninstall
```

### Remove Source Code

```bash
rm -rf ~/Builder
```

---

## 📊 Build Output

After successful build, you'll see:

```
╔════════════════════════════════════════════════════════╗
║                  Installation Complete!                ║
╚════════════════════════════════════════════════════════╝

[INFO] Builder app is now installed on your device
[INFO] Package name: com.builder

[INFO] Next steps:
  1. Open the Builder app from your app drawer
  2. Grant required permissions
  3. Navigate to 'GitHub Packs'
  4. Sign in with GitHub to browse repositories

[INFO] To rebuild and reinstall:
  $ ./install.sh
```

---

## 🆘 Getting Help

### Check Logs

Build logs are saved to `build.log`:
```bash
tail -100 build.log
```

### Common Issues

| Error | Solution |
|-------|----------|
| `java: command not found` | `pkg install openjdk-17` |
| `adb: no devices found` | Enable USB debugging |
| `Out of memory` | Increase `org.gradle.jvmargs` |
| `APK not found` | Run `./install.sh --build-only` first |

### Report Issues

Found a bug? Report it:
- **GitHub Issues**: https://github.com/RachEma-ux/Builder/issues
- **Include**: Build logs, error messages, device info

---

## 🎓 Advanced Usage

### Custom Build Configuration

Edit `gradle.properties`:
```properties
# Increase memory
org.gradle.jvmargs=-Xmx4096m

# Enable parallel builds
org.gradle.parallel=true

# Enable build cache
org.gradle.caching=true
```

### Build Variants

```bash
# Debug build (default)
./gradlew assembleDebug

# Release build (requires signing)
./gradlew assembleRelease
```

### Run Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device)
./gradlew connectedAndroidTest
```

### Install on Multiple Devices

```bash
# List connected devices
adb devices

# Install on specific device
adb -s DEVICE_ID install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌟 Features Overview

### Current Features (v1.0.0)

- ✅ GitHub OAuth authentication
- ✅ Repository browsing
- ✅ Pack installation from:
  - GitHub Releases
  - Workflow Artifacts
  - Branches/Tags
- ✅ Workflow engine execution:
  - HTTP requests
  - KV store operations
  - Logging
  - Sleep/delay
- ✅ Real-time logs viewer
- ✅ Health monitoring (CPU, memory, network)
- ✅ Instance lifecycle management

### Coming Soon

- ⏳ WASM pack execution (requires Wasmtime build)
- ⏳ Pack marketplace
- ⏳ Cloud sync
- ⏳ Scheduled workflows

---

## 📝 Summary

| Command | Purpose |
|---------|---------|
| `./install.sh` | Build and install (default) |
| `./install.sh --build-only` | Just build, don't install |
| `./install.sh --install-only` | Just install existing APK |
| `./install.sh --help` | Show help |

**That's it!** You now have Builder installed and running on your Android device. 🎉

---

**Last Updated**: 2026-01-10
**Version**: 1.0.0
**Tested On**: Termux (F-Droid), Android 8.0+
