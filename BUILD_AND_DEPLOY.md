# Builder - Build and Deployment Guide

**Last Updated:** 2026-01-16
**Status:** ✅ GitHub Actions Configured and Ready

---

## ⚡ **CRITICAL: Builds Happen on GitHub Actions (NOT Locally!)**

**DO NOT attempt to build locally unless you are actively developing in Android Studio.**

Builder is designed to be built on **GitHub's servers** using GitHub Actions. This ensures:
- ✅ Consistent build environment
- ✅ No local SDK/NDK setup required
- ✅ Automatic dependency management
- ✅ Build artifacts automatically stored and versioned
- ✅ Works even in restricted network environments

---

## 📋 GitHub Actions IS Already Set Up

Here's the **correct build & deployment flow**:

---

## 📦 BUILD PROCESS (Where APK is Created)

### ✅ GitHub Actions (Automated - Already Configured!)

```
┌─────────────────────────────────────────────────┐
│  YOU PUSH CODE TO GITHUB                        │
│  (git push origin branch-name)                  │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  GITHUB ACTIONS RUNS (on GitHub servers)        │
│  - Sets up Android SDK                          │
│  - Downloads dependencies (AGP, libraries)      │
│  - Runs tests                                   │
│  - Builds Debug APK                             │
│  - Builds Release APK                           │
│  - Runs Lint checks                             │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  ARTIFACTS UPLOADED (APK files)                 │
│  - builder-debug-apk (30 days retention)        │
│  - builder-release-apk (90 days retention)      │
│  - test-results                                 │
│  - lint-reports                                 │
└─────────────────────────────────────────────────┘
```

### Build Triggers

GitHub Actions automatically builds when you push to:

- ✅ **`main`** branch
- ✅ **`develop`** branch
- ✅ **`claude/**`** branches (any branch starting with `claude/`)
- ✅ Pull requests to `main` or `develop`
- ✅ Manual trigger via GitHub UI (workflow_dispatch)

**Example:** Your current branch `claude/list-builder-branches-SduQO` WILL trigger the build automatically when pushed!

---

## 📲 DEPLOYMENT PROCESS (Installing APK on Device)

```
┌─────────────────────────────────────────────────┐
│  DOWNLOAD APK from GitHub Actions               │
│  (Go to Actions tab → Select workflow run)      │
└─────────────────┬───────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────┐
│  INSTALL ON DEVICE                              │
│  Method 1: adb install app-debug.apk           │
│  Method 2: Transfer to phone & open             │
│  Method 3: Play Store (for production)          │
└─────────────────────────────────────────────────┘
```

### Method 1: Using ADB (Recommended for Developers)

```bash
# Download APK from GitHub Actions
# Then install using adb
adb install -r app-debug.apk
```

### Method 2: Direct Install (Recommended for Testing)

1. Download APK from GitHub Actions to your computer
2. Transfer file to your Android device (USB, cloud storage, etc.)
3. Open the APK file on your device
4. Grant "Install Unknown Apps" permission if prompted
5. Tap "Install"

### Method 3: Play Store (Production Only)

For production releases, APKs will be uploaded to Google Play Store.

---

## 🔍 Why Local Builds May Fail (And Why It Doesn't Matter)

### Common Local Build Issues:

❌ **Network Restrictions**
- Cannot download Android Gradle Plugin
- Cannot download dependencies
- Cannot download Android SDK components

❌ **Missing Dependencies**
- Android SDK not installed
- Android NDK not installed
- Wrong Java version

❌ **Environment Issues**
- Firewall blocking Gradle
- Proxy configuration needed
- Corporate network restrictions

### ✅ **Why It Doesn't Matter:**

**Because builds don't need to happen locally!**

GitHub Actions has:
- ✅ Full internet access
- ✅ Android SDK pre-installed
- ✅ All dependencies cached
- ✅ Reliable build environment
- ✅ No manual setup required

---

## 📝 Step-by-Step: Complete Workflow

### For New Code Changes:

1. **Make your code changes locally**
   ```bash
   # Edit files in your IDE
   git add .
   git commit -m "feat: Add new feature"
   ```

2. **Push to GitHub**
   ```bash
   git push origin your-branch-name
   ```

3. **Monitor GitHub Actions**
   - Go to: https://github.com/RachEma-ux/Builder/actions
   - Find your workflow run
   - Watch the build progress (takes ~5-10 minutes)

4. **Download the APK**
   - Click on the successful workflow run
   - Scroll to "Artifacts" section
   - Download `builder-debug-apk.zip`
   - Extract the APK file

5. **Install on your device**
   - Use ADB or direct transfer
   - Test your changes

6. **Iterate**
   - Make more changes
   - Repeat from step 1

---

## 🎯 Build Workflow Details

### Workflow File Location

`.github/workflows/android-build.yml`

### What Gets Built:

1. **Debug APK** (`app-debug.apk`)
   - Debuggable
   - Not optimized
   - Retained for 30 days
   - Use for: Development and testing

2. **Release APK** (`app-release.apk`)
   - Optimized (ProGuard/R8)
   - Smaller file size
   - Retained for 90 days
   - Use for: Production deployment

### What Gets Tested:

1. **Unit Tests** - Domain logic tests
2. **Lint Checks** - Code quality analysis
3. **Build Verification** - Ensures project compiles

---

## 🚨 Troubleshooting

### "Build failed" on GitHub Actions

**Check the logs:**
1. Go to Actions tab
2. Click on the failed run
3. Expand the failed step
4. Read the error message

**Common issues:**
- Syntax errors in code
- Missing dependencies in build.gradle
- Test failures
- Lint errors

### "Can't download APK artifact"

**Make sure:**
- The workflow completed successfully (green checkmark)
- You're looking at the right workflow run
- Artifacts haven't expired (30 or 90 days)

### "APK won't install on device"

**Check:**
- "Install Unknown Apps" permission enabled
- Enough storage space on device
- Previous version uninstalled (if signature mismatch)
- Using correct APK for your device architecture

---

## 📊 Artifact Retention Policy

| Artifact | Retention | Use Case |
|----------|-----------|----------|
| **builder-debug-apk** | 30 days | Development, testing, CI |
| **builder-release-apk** | 90 days | Production releases, archival |
| **test-results** | 30 days | Test reports, debugging |
| **lint-reports** | 30 days | Code quality analysis |

---

## 🔐 Production Release Process

### Creating a Production Release:

1. **Create and push a tag:**
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. **GitHub Actions builds release APK**

3. **Download release APK from artifacts**

4. **Sign the APK** (if not already signed in workflow)
   ```bash
   jarsigner -verbose -sigalg SHA256withRSA \
     -digestalg SHA-256 \
     -keystore my-release-key.keystore \
     app-release.apk alias_name
   ```

5. **Upload to Play Store** or distribute directly

---

## 💡 Best Practices

### ✅ DO:
- Always push to GitHub and let Actions build
- Download APK from Actions artifacts
- Test on real devices, not just emulators
- Check build logs for warnings
- Keep branches up to date with main

### ❌ DON'T:
- Don't try to build locally unless developing in Android Studio
- Don't distribute unsigned APKs to users
- Don't ignore lint warnings
- Don't skip tests
- Don't commit sensitive data (keys, passwords)

---

## 🔗 Quick Links

- **GitHub Actions**: https://github.com/RachEma-ux/Builder/actions
- **Issues**: https://github.com/RachEma-ux/Builder/issues
- **Workflow File**: [.github/workflows/android-build.yml](.github/workflows/android-build.yml)
- **README**: [README.md](README.md)
- **Developer Guide**: [DEVELOPER_QUICKSTART.md](DEVELOPER_QUICKSTART.md)

---

## ❓ FAQ

### Q: Do I need Android Studio installed?
**A:** Only if you're actively developing (editing code). For just building, use GitHub Actions.

### Q: Why can't I build locally?
**A:** You can, but it requires Android SDK setup and internet access. GitHub Actions is easier and more reliable.

### Q: How long do builds take?
**A:** Typically 5-10 minutes on GitHub Actions. First build may take longer (caching dependencies).

### Q: Can I build without pushing to GitHub?
**A:** Technically yes with local setup, but not recommended. Use GitHub Actions for consistency.

### Q: What if I don't have an Android device?
**A:** Use Android Emulator in Android Studio, or test via GitHub Actions only.

---

## 📞 Need Help?

- **Build Issues**: Check [GitHub Actions logs](https://github.com/RachEma-ux/Builder/actions)
- **Installation Issues**: See [INSTALL.md](INSTALL.md)
- **Development Setup**: See [DEVELOPER_QUICKSTART.md](DEVELOPER_QUICKSTART.md)
- **Report Bugs**: [GitHub Issues](https://github.com/RachEma-ux/Builder/issues)

---

**Remember: GitHub Actions is your friend. Let it do the building for you!** 🚀
