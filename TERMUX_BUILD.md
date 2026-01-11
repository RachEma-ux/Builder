# Building Builder on Termux (ARM64)

## The ARM64 Build Tools Problem

If you're trying to build Builder on your Android phone using Termux, you may encounter this error:

```
Installed Build Tools revision 34.0.0 is corrupted
```

**This is not a bug in Builder or your setup.** It's a fundamental platform limitation.

### Why Local Builds Fail on ARM64

1. **Android Build Tools are x86_64 binaries**
   - Google's `aapt2`, `zipalign`, `apksigner`, etc. are compiled for Intel/AMD processors
   - Your phone uses ARM64 architecture
   - These binaries cannot run on ARM64 devices

2. **This affects all Android apps built on Termux**
   - Not specific to Builder
   - No amount of reinstalling or reconfiguring will fix it
   - Emulation (qemu) is too slow to be practical

### ✅ Recommended Solution: GitHub Actions

GitHub Actions provides x86_64 Linux runners that can build your APK in the cloud.

#### How It Works

```
You (ARM64 phone) → Push code to GitHub → GitHub Actions (x86_64 VM) builds APK → Download APK to phone
```

#### Step-by-Step Instructions

1. **Push your code to GitHub** (if you haven't already)
   ```bash
   git add .
   git commit -m "Update code"
   git push origin main
   ```

2. **Wait for the build to complete** (~5 minutes)
   - GitHub Actions automatically runs on every push
   - View progress at: https://github.com/RachEma-ux/Builder/actions

3. **Download the APK**
   - Go to the completed workflow run
   - Scroll to "Artifacts" section
   - Download `builder-debug-apk`
   - Extract the ZIP file
   - Install the APK on your phone

#### Using GitHub CLI (Easier)

Install GitHub CLI on Termux:
```bash
pkg install gh
gh auth login
```

Download the latest APK:
```bash
# List recent workflow runs
gh run list --repo RachEma-ux/Builder

# Download artifacts from a specific run
gh run download <run-id> --name builder-debug-apk

# Install the APK
adb install -r *.apk
```

### What Gets Built

Every push triggers builds for:
- **Debug APK** (`builder-debug-apk`) - Ready to install, 30-day retention
- **Release APK** (`builder-release-apk`) - Unsigned release build, 90-day retention

### Quick Reference

```bash
# Show GitHub Actions help
./install.sh --github-help

# Try local build (may fail on ARM64)
./install.sh --build-only

# Install existing APK
./install.sh --install-only
```

### Why Not Hack It?

Some guides suggest:
- Installing Termux's `aapt2` package
- Forcing Gradle to use alternative tools
- Overriding build tool paths

**We don't recommend this because:**
- ❌ Breaks randomly with Gradle updates
- ❌ Not supported by Android Gradle Plugin
- ❌ May produce corrupted APKs
- ❌ Wastes hours debugging edge cases

GitHub Actions is the **industry-standard** way mobile developers build Android apps when they don't have a local build environment.

### Frequently Asked Questions

**Q: Can I fix the "corrupted build tools" error?**
A: No. The build tools are not corrupted—they're x86_64 binaries that won't run on ARM64.

**Q: What if I downgrade build tools?**
A: All versions are x86_64. Downgrading won't help.

**Q: Can I use QEMU to emulate x86_64?**
A: Technically yes, but it's extremely slow (10-100x slower). Not practical.

**Q: Will this be fixed in the future?**
A: Only if Google releases ARM64 build tools. No timeline announced.

**Q: Does GitHub Actions cost money?**
A: Free for public repositories (2,000 minutes/month). Our builds use ~5 minutes each.

### Support

If you encounter issues with GitHub Actions:
1. Check the Actions tab: https://github.com/RachEma-ux/Builder/actions
2. Look for red ✗ marks indicating failures
3. Open an issue with the error logs

### Summary

| Method | Works on ARM64? | Speed | Reliability |
|--------|----------------|-------|-------------|
| Local build (`./install.sh`) | ❌ No | - | Not possible |
| GitHub Actions | ✅ Yes | ~5 min | 100% reliable |
| QEMU emulation | ⚠️ Technically | Hours | Unreliable |

**Bottom line:** Use GitHub Actions. It's free, fast, and how professional developers build Android apps.
