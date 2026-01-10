# GitHub OAuth App Verification Guide

This guide helps you verify that your GitHub OAuth application is correctly registered and configured for Builder.

## Current Status Check

**CLIENT_ID in Code**: `Ov23liPLACEHOLDER_UPDATE_ME`
**Status**: ⚠️ **PLACEHOLDER** - Not yet configured

## ✅ Step-by-Step Verification

### Step 1: Check if OAuth App is Registered

#### Option A: Via GitHub Web Interface

1. **Navigate to GitHub Settings**:
   ```
   https://github.com/settings/developers
   ```

2. **Click "OAuth Apps"** in the left sidebar

3. **Look for your Builder application**:
   - Name should be: `Builder - Mobile Orchestration` (or similar)
   - If you see it: ✅ **App is registered**
   - If you don't see it: ❌ **App is NOT registered** → Go to Step 2

#### Option B: Via Command Line (using GitHub CLI)

```bash
# Install GitHub CLI if not already installed
# brew install gh  # macOS
# sudo apt install gh  # Ubuntu/Debian

# Login to GitHub
gh auth login

# List OAuth apps (requires GraphQL query)
gh api graphql -f query='
{
  viewer {
    login
    oauthApplications(first: 10) {
      nodes {
        name
        clientId
        url
      }
    }
  }
}'
```

### Step 2: Register OAuth App (if not already registered)

If you don't have an OAuth app, create one:

1. **Go to registration page**:
   ```
   https://github.com/settings/applications/new
   ```

2. **Fill in the form**:

   | Field | Value |
   |-------|-------|
   | **Application name** | `Builder - Mobile Orchestration` |
   | **Homepage URL** | `https://github.com/RachEma-ux/Builder` |
   | **Application description** | `Non-root smartphone orchestration system for Android` |
   | **Authorization callback URL** | `builder://oauth/callback` |

3. **Click "Register application"**

4. **You'll be redirected to the app page** showing:
   - ✅ Client ID (starts with `Ov` or `Iv1.`)
   - 🔐 Client Secret (optional, not needed for device flow)

### Step 3: Verify OAuth App Configuration

Once registered, verify the settings:

#### Required Settings

| Setting | Expected Value | Verification |
|---------|---------------|--------------|
| **Client ID** | Starts with `Ov` or `Iv1.` | ✅ Copy this value |
| **Callback URL** | `builder://oauth/callback` | ⚠️ Must match exactly |
| **Device Flow** | Enabled (default) | ✅ Enabled by default |
| **Active** | Yes (green badge) | ⚠️ Check not suspended |

#### Callback URL Verification

The callback URL **MUST** be exactly:
```
builder://oauth/callback
```

**Common mistakes**:
- ❌ `builder:/oauth/callback` (missing one slash)
- ❌ `builder://oauth/` (missing callback)
- ❌ `https://builder.app/oauth/callback` (wrong scheme)
- ❌ `builder://oauth/callback/` (extra trailing slash)

### Step 4: Copy Client ID

1. **Find your Client ID** on the OAuth app page
   - Should look like: `Ov23liA1B2C3D4E5F6G7H8`
   - Or older format: `Iv1.1234567890abcdef`

2. **Copy the Client ID** (click the copy icon or select and copy)

3. **Keep the browser tab open** (you'll need to verify it works)

### Step 5: Update Builder Code

#### Option A: Direct Update (Quick Start)

1. **Open the file**:
   ```bash
   nano data/remote/github/GitHubOAuthService.kt
   ```

2. **Find line 31**:
   ```kotlin
   const val CLIENT_ID = "Ov23liPLACEHOLDER_UPDATE_ME"
   ```

3. **Replace with your Client ID**:
   ```kotlin
   const val CLIENT_ID = "Ov23liYourActualClientId"  // ✅ Real ID
   ```

4. **Save the file**

#### Option B: BuildConfig (Production Approach)

**Step 1**: Create `local.properties` (not committed to git):
```bash
echo "githubClientId=Ov23liYourActualClientId" > local.properties
```

**Step 2**: Update `app/build.gradle.kts`:
```kotlin
import java.util.Properties

android {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    defaultConfig {
        // ...
        buildConfigField(
            "String",
            "GITHUB_CLIENT_ID",
            "\"${localProperties.getProperty("githubClientId", "Ov23liPLACEHOLDER")}\""
        )
    }
}
```

**Step 3**: Update `GitHubOAuthService.kt`:
```kotlin
import com.builder.BuildConfig

companion object {
    const val BASE_URL = "https://github.com/"
    val CLIENT_ID = BuildConfig.GITHUB_CLIENT_ID
}
```

### Step 6: Verify Configuration

#### 6.1 Verify CLIENT_ID is Updated

```bash
# Check the file
grep "CLIENT_ID" data/remote/github/GitHubOAuthService.kt

# Should show your actual Client ID, not the placeholder
# ✅ const val CLIENT_ID = "Ov23liA1B2C3..."
# ❌ const val CLIENT_ID = "Ov23liPLACEHOLDER_UPDATE_ME"
```

#### 6.2 Build the App

```bash
./gradlew clean assembleDebug
```

**Expected output**:
```
BUILD SUCCESSFUL in 45s
```

**If build fails**:
- Check syntax errors in `GitHubOAuthService.kt`
- Ensure Client ID is wrapped in quotes
- Verify no special characters

#### 6.3 Test OAuth Flow (Manual)

1. **Install the app**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Launch the app**

3. **Navigate to "GitHub Packs" screen**

4. **Tap "Sign in with GitHub"**

5. **You should see**:
   - ✅ A device code (e.g., `1A2B-3C4D`)
   - ✅ Verification URL: `https://github.com/login/device`
   - ❌ If you see "invalid_client" → Client ID is wrong

6. **Open browser** and go to the URL

7. **Enter the device code**

8. **Authorize the application**

9. **Return to app**:
   - ✅ You should be signed in
   - ✅ See your GitHub repositories
   - ❌ If stuck on "Waiting..." → Check network/firewall

### Step 7: Automated Verification Script

Create a test script to verify OAuth without the app:

```bash
#!/bin/bash
# test-oauth.sh

CLIENT_ID="Ov23liYourActualClientId"  # Replace with your ID

echo "Testing GitHub OAuth Device Flow..."
echo "Client ID: $CLIENT_ID"

# Step 1: Get device code
RESPONSE=$(curl -s -X POST \
  -H "Accept: application/json" \
  -d "client_id=$CLIENT_ID&scope=repo,workflow" \
  https://github.com/login/device/code)

echo "Device Code Response:"
echo "$RESPONSE" | jq .

# Check for errors
if echo "$RESPONSE" | jq -e '.error' > /dev/null; then
  echo "❌ ERROR: $(echo "$RESPONSE" | jq -r '.error_description')"
  exit 1
fi

# Extract device code
DEVICE_CODE=$(echo "$RESPONSE" | jq -r '.device_code')
USER_CODE=$(echo "$RESPONSE" | jq -r '.user_code')
VERIFICATION_URI=$(echo "$RESPONSE" | jq -r '.verification_uri')

echo ""
echo "✅ OAuth app is correctly configured!"
echo ""
echo "Device Code: $DEVICE_CODE"
echo "User Code: $USER_CODE"
echo "Verification URI: $VERIFICATION_URI"
echo ""
echo "To complete authorization:"
echo "1. Open: $VERIFICATION_URI"
echo "2. Enter code: $USER_CODE"
```

**Run the script**:
```bash
chmod +x test-oauth.sh
./test-oauth.sh
```

**Expected output** (success):
```
✅ OAuth app is correctly configured!
Device Code: 1a2b3c4d5e6f...
User Code: 1A2B-3C4D
Verification URI: https://github.com/login/device
```

**Error outputs**:
```json
// ❌ Invalid Client ID
{
  "error": "invalid_client",
  "error_description": "The client_id passed is invalid."
}

// ❌ Invalid Scope
{
  "error": "invalid_scope",
  "error_description": "The scope requested is invalid."
}
```

## ✅ Verification Checklist

Use this checklist to confirm everything is set up correctly:

- [ ] **OAuth app registered** on GitHub
- [ ] **Application name** is descriptive
- [ ] **Callback URL** is exactly `builder://oauth/callback`
- [ ] **Client ID** copied from GitHub app page
- [ ] **Client ID** starts with `Ov` or `Iv1.`
- [ ] **CLIENT_ID constant** updated in code (no placeholder)
- [ ] **Code builds** without errors (`./gradlew assembleDebug`)
- [ ] **Manual test** shows device code (not "invalid_client")
- [ ] **Automated test** (curl script) returns device code
- [ ] **App OAuth flow** works end-to-end
- [ ] **Can browse** GitHub repositories after login

## 🔍 Common Issues

### Issue 1: "invalid_client" Error

**Symptoms**: Device flow returns error immediately

**Causes**:
1. Client ID is incorrect/typo
2. Client ID is still the placeholder
3. OAuth app was deleted
4. OAuth app is suspended

**Fix**:
```bash
# Verify Client ID in code matches GitHub
grep "CLIENT_ID" data/remote/github/GitHubOAuthService.kt

# Compare with GitHub settings
open https://github.com/settings/developers
```

### Issue 2: "invalid_scope" Error

**Symptoms**: Device flow returns scope error

**Cause**: Requesting scopes the app doesn't have

**Fix**: Check scopes in `GitHubOAuthService.kt` line 43:
```kotlin
@Field("scope") scope: String = "repo,workflow"
```

**Valid scopes**:
- `repo` - Repository access ✅
- `workflow` - GitHub Actions ✅
- `read:org` - Organization read ✅
- `gist` - Gist access (optional)

### Issue 3: Callback URL Mismatch

**Symptoms**: OAuth works but app doesn't receive token

**Cause**: Callback URL doesn't match Android deep link

**Fix**:
1. Verify `AndroidManifest.xml` has:
   ```xml
   <intent-filter>
       <action android:name="android.intent.action.VIEW"/>
       <category android:name="android.intent.category.DEFAULT"/>
       <category android:name="android.intent.category.BROWSABLE"/>
       <data android:scheme="builder"
             android:host="oauth"
             android:pathPrefix="/callback"/>
   </intent-filter>
   ```

2. Verify GitHub OAuth app callback is: `builder://oauth/callback`

### Issue 4: Build Fails After Update

**Symptoms**: Compilation error in GitHubOAuthService.kt

**Cause**: Syntax error in Client ID

**Fix**:
```kotlin
// ✅ Correct
const val CLIENT_ID = "Ov23liA1B2C3D4E5"

// ❌ Wrong - Missing quotes
const val CLIENT_ID = Ov23liA1B2C3D4E5

// ❌ Wrong - Extra quotes
const val CLIENT_ID = ""Ov23liA1B2C3D4E5""
```

## 📊 Verification Report Template

After verification, fill out this report:

```markdown
## GitHub OAuth Verification Report

**Date**: 2026-01-10
**Tester**: [Your Name]

### OAuth App Registration
- [ ] App registered: Yes/No
- [ ] App name: _________________
- [ ] Client ID: Ov______________ (first 4 chars)
- [ ] Callback URL: builder://oauth/callback

### Code Configuration
- [ ] CLIENT_ID updated: Yes/No
- [ ] Build succeeds: Yes/No
- [ ] Test script passes: Yes/No

### Functional Testing
- [ ] Device flow initiated: Yes/No
- [ ] Device code received: Yes/No
- [ ] Authorization successful: Yes/No
- [ ] Token stored: Yes/No
- [ ] Repository list loads: Yes/No

### Result
- [ ] ✅ PASSED - OAuth fully configured
- [ ] ⚠️ PARTIAL - Some issues remain
- [ ] ❌ FAILED - Not configured

**Notes**: _______________
```

## 🚀 Next Steps After Verification

Once OAuth is verified:

1. **Commit the change** (if using direct update):
   ```bash
   git add data/remote/github/GitHubOAuthService.kt
   git commit -m "Configure GitHub OAuth client ID"
   ```

2. **Or add to .gitignore** (if using BuildConfig):
   ```bash
   echo "local.properties" >> .gitignore
   git add .gitignore
   git commit -m "Add local.properties to gitignore"
   ```

3. **Test pack installation**:
   - Sign in with GitHub
   - Browse repositories
   - Select a pack repository
   - Install in Dev mode

4. **Enable production features**:
   - Follow Builder_Final.md §2 for production index
   - Test Prod mode installation

## 📚 Resources

- **GitHub OAuth Docs**: https://docs.github.com/en/apps/oauth-apps
- **Device Flow Spec**: https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow
- **Builder OAuth Guide**: `docs/GITHUB_OAUTH_SETUP.md`
- **Developer Quickstart**: `DEVELOPER_QUICKSTART.md`

---

**Last Updated**: 2026-01-10
