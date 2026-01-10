# GitHub OAuth Setup Guide for Builder

This guide explains how to register a GitHub OAuth application for Builder and configure the client ID.

## Overview

Builder uses GitHub OAuth Device Flow to allow users to:
- Browse their GitHub repositories
- Select packs from repos and branches
- Trigger GitHub Actions workflows to build packs
- Download workflow artifacts

## Prerequisites

- A GitHub account
- Owner or admin access to the org/repo where you'll register the app
- 10 minutes of your time

## Step 1: Register GitHub OAuth App

### 1.1 Navigate to GitHub Settings

1. Go to **GitHub.com**
2. Click your profile picture (top right)
3. Select **Settings**
4. Scroll down to **Developer settings** (bottom of left sidebar)
5. Click **OAuth Apps**
6. Click **New OAuth App**

**Direct Link**: https://github.com/settings/applications/new

### 1.2 Fill in Application Details

**Application name**:
```
Builder - Mobile Orchestration
```

**Homepage URL**:
```
https://github.com/RachEma-ux/Builder
```
(Or your fork URL)

**Application description** (optional):
```
Non-root smartphone orchestration system. Allows installing and running WASM/Workflow packs directly on Android devices.
```

**Authorization callback URL**:
```
builder://oauth/callback
```

**Important**: This is the custom URI scheme that Builder uses. Do NOT change it unless you also update the AndroidManifest.xml.

### 1.3 Register the Application

1. Click **Register application**
2. You'll be redirected to the app's page
3. **Copy the Client ID** (looks like: `Iv1.1234567890abcdef`)

### 1.4 Generate Client Secret (Optional)

**Note**: For the OAuth Device Flow, Builder does NOT need the client secret. The client ID is sufficient.

However, if you plan to use other OAuth flows in the future:
1. Click **Generate a new client secret**
2. Copy and save it securely (you won't be able to see it again)
3. Store in Android EncryptedSharedPreferences (not in code!)

## Step 2: Configure Builder

### 2.1 Update GitHubApiService.kt

Open the file:
```
data/remote/github/GitHubApiService.kt
```

Find the `GITHUB_CLIENT_ID` constant (around line 20):

**Before**:
```kotlin
private const val GITHUB_CLIENT_ID = "Iv1.placeholder_update_me"
```

**After** (use your actual client ID):
```kotlin
private const val GITHUB_CLIENT_ID = "Iv1.1234567890abcdef"
```

### 2.2 Alternative: Use BuildConfig

For better security, use BuildConfig to keep the client ID out of version control:

**Step 1**: Update `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        // ...
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"Iv1.1234567890abcdef\"")
    }
}
```

**Step 2**: Update `GitHubApiService.kt`:
```kotlin
private const val GITHUB_CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
```

**Step 3**: Add to `.gitignore`:
```
local.properties
```

**Step 4**: Create `local.properties`:
```properties
githubClientId=Iv1.1234567890abcdef
```

**Step 5**: Read in `build.gradle.kts`:
```kotlin
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    defaultConfig {
        buildConfigField(
            "String",
            "GITHUB_CLIENT_ID",
            "\"${localProperties.getProperty("githubClientId", "Iv1.placeholder")}\""
        )
    }
}
```

## Step 3: Test OAuth Flow

### 3.1 Build and Run

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3.2 Test Device Flow

1. Open Builder app
2. Navigate to **GitHub Packs** screen
3. Tap **Sign in with GitHub**
4. You should see:
   - A device code (e.g., `1A2B-3C4D`)
   - A URL (https://github.com/login/device)
5. Open the URL in a browser
6. Enter the device code
7. Authorize the application
8. Return to the app
9. You should be signed in and see your repositories

### 3.3 Verify Token Storage

After successful login, check that the token is stored securely:

```bash
adb shell
run-as com.builder
ls shared_prefs/
# Should show: github_auth_encrypted_prefs.xml
```

## Step 4: Update App Permissions (Optional)

By default, Builder requests these OAuth scopes:
- `repo` (read/write repository access)
- `workflow` (trigger GitHub Actions)
- `read:org` (read organization membership)

To change scopes, update `GitHubRepository.kt`:

```kotlin
private val OAUTH_SCOPES = listOf("repo", "workflow", "read:org")
```

Available scopes: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps

## Security Considerations

### Do NOT:
- ❌ Commit client secret to git
- ❌ Share client secret publicly
- ❌ Use production client ID in development builds
- ❌ Store tokens in plain text

### DO:
- ✅ Use EncryptedSharedPreferences for tokens (already implemented)
- ✅ Keep client ID in BuildConfig or environment variables
- ✅ Use separate OAuth apps for dev/staging/production
- ✅ Regularly rotate client secrets (if used)
- ✅ Monitor authorized users in GitHub settings

### Revoking Access

Users can revoke access at any time:
1. Go to https://github.com/settings/applications
2. Find "Builder - Mobile Orchestration"
3. Click **Revoke**

Your app should handle 401 responses and prompt for re-authentication.

## Troubleshooting

### Error: "The client_id passed is invalid"

**Cause**: Client ID is incorrect or app was deleted

**Fix**:
1. Verify client ID in GitHub settings
2. Ensure no typos in `GitHubApiService.kt`
3. Rebuild app: `./gradlew clean assembleDebug`

### Error: "Device authorization failed"

**Cause**: User denied authorization or timed out

**Fix**:
1. Retry the device flow
2. Ensure user has access to the org/repos
3. Check app scopes match what user can grant

### Error: "Could not resolve host: github.com"

**Cause**: Network connectivity issue

**Fix**:
1. Check device internet connection
2. Ensure app has INTERNET permission (already in manifest)
3. Check if behind corporate firewall (may block OAuth)

### Token Expired

OAuth tokens expire after 8 hours by default. Builder automatically refreshes using the device flow.

If seeing frequent logouts:
1. Check `GitHubRepository.kt` refresh logic
2. Ensure EncryptedSharedPreferences is working
3. Check device logs: `adb logcat | grep GitHub`

## Production Deployment

### For App Store Release:

1. **Create Production OAuth App**:
   - Use company email/org account
   - Name: "Builder - Mobile Orchestration (Production)"
   - Document app credentials securely

2. **Configure Release Build**:
   ```kotlin
   // app/build.gradle.kts
   buildTypes {
       release {
           buildConfigField(
               "String",
               "GITHUB_CLIENT_ID",
               "\"${System.getenv("GITHUB_CLIENT_ID_PROD") ?: "error"}\""
           )
       }
   }
   ```

3. **Set Environment Variable in CI/CD**:
   ```bash
   # GitHub Actions
   env:
     GITHUB_CLIENT_ID_PROD: ${{ secrets.GITHUB_CLIENT_ID_PROD }}
   ```

4. **Monitor Usage**:
   - Check GitHub app analytics
   - Monitor rate limits (5000 req/hour)
   - Set up alerts for failures

## API Rate Limits

GitHub API rate limits:
- **Authenticated**: 5,000 requests/hour
- **Unauthenticated**: 60 requests/hour

Builder counts against your user's rate limit. The app displays remaining requests in the UI (see `GitHubPacksViewModel`).

If hitting limits:
1. Implement caching (already partially done)
2. Reduce polling frequency
3. Use GraphQL API (fewer requests)
4. Request rate limit increase from GitHub

## Resources

- **GitHub OAuth Documentation**: https://docs.github.com/en/apps/oauth-apps
- **Device Flow Specification**: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow
- **OAuth Scopes**: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps
- **Builder Issue Tracker**: https://github.com/RachEma-ux/Builder/issues

## Support

For issues:
1. Check logs: `adb logcat | grep -E "GitHub|OAuth"`
2. Enable debug logging in `GitHubRepository.kt`
3. Open issue at: https://github.com/RachEma-ux/Builder/issues
4. Include: device, Android version, error message, steps to reproduce

---

**Last Updated**: 2026-01-10
**OAuth Version**: Device Flow (RFC 8628)
**Minimum Android API**: 26
