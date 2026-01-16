# OAuth Authorization Code Flow Testing Guide

## Overview
This guide provides comprehensive testing instructions for the new OAuth Authorization Code Flow with PKCE implementation.

## Prerequisites
- Android device or emulator (API 26+)
- GitHub account
- Android Studio or command-line build tools

## Build Instructions

### Option 1: Android Studio
1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Click "Run" (▶️) or use `Shift+F10`
4. Select your device/emulator

### Option 2: Command Line
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Test Cases

### Test 1: Successful OAuth Flow (Happy Path)

**Steps:**
1. Launch the Builder app
2. You should see "GitHub Authentication Required" screen
3. Tap "Connect GitHub" button
4. **Expected:** Browser opens automatically
5. **Expected:** You see GitHub's authorization page with:
   - "Authorize Builder" heading
   - Permissions list (repo, workflow)
   - Green "Authorize" button
6. Tap the "Authorize" button on GitHub
7. **Expected:** Browser redirects back to Builder app automatically
8. **Expected:** App shows:
   - "✓ Authenticated successfully!"
   - Repository list loads
   - "Dev (Branches)" and "Production (Tags)" tabs appear

**Success Criteria:**
- ✅ No manual code entry required
- ✅ Seamless redirect back to app
- ✅ Authentication completes in <5 seconds after authorization
- ✅ Repositories load automatically

**Logs to Check:**
```
adb logcat | grep -E "OAuth|GitHubOAuth"
```

Expected log output:
```
Opening browser for OAuth authorization: https://github.com/login/oauth/authorize?...
Received OAuth callback: builder://oauth/callback?code=...&state=...
Processing OAuth authorization code
Successfully obtained access token via authorization code flow
```

---

### Test 2: User Cancellation

**Steps:**
1. Launch app (fresh install or after logout)
2. Tap "Connect GitHub"
3. When browser opens, tap "Cancel" on GitHub's authorization page
4. **Expected:** Return to app
5. **Expected:** Error message appears: "Authorization timed out. Please try again."
6. Tap "Retry" button
7. **Expected:** OAuth flow restarts

**Success Criteria:**
- ✅ App handles cancellation gracefully
- ✅ Error message is clear
- ✅ Retry works correctly

---

### Test 3: Network Error Handling

**Steps:**
1. Enable airplane mode
2. Launch app
3. Tap "Connect GitHub"
4. **Expected:** Error message: "Unknown error" or connection error
5. Disable airplane mode
6. Tap "Retry"
7. **Expected:** OAuth flow completes successfully

**Success Criteria:**
- ✅ No app crash
- ✅ Clear error message
- ✅ Retry recovers from error

---

### Test 4: Deep Link Verification

**Steps:**
1. Start OAuth flow
2. While on GitHub authorization page, check the redirect URI
3. **Expected:** Deep link shows `redirect_uri=builder://oauth/callback`
4. After authorization, use `adb logcat` to verify deep link was received

**Success Criteria:**
- ✅ Deep link format is correct
- ✅ MainActivity receives and processes the deep link
- ✅ Code and state parameters are extracted correctly

**Manual deep link test:**
```bash
# After starting OAuth flow, manually trigger deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "builder://oauth/callback?code=test123&state=test456"
```

---

### Test 5: PKCE Security Verification

**Steps:**
1. Enable verbose logging
2. Start OAuth flow
3. Check logs for PKCE parameters

**Expected Log Entries:**
```
Generated code verifier: [43+ character string]
Generated code challenge: [43+ character string]
State parameter: [22+ character string]
```

**Success Criteria:**
- ✅ Code verifier is cryptographically random (43-128 chars)
- ✅ Code challenge is SHA-256 hash of verifier
- ✅ State is random and unique per request
- ✅ Verifier and state are cleared after successful auth

**Verify secure storage:**
```bash
# After starting OAuth (before callback), check encrypted prefs exist
adb shell run-as com.builder ls -la shared_prefs/
# Should see github_oauth_prefs.xml

# After successful auth, verify code_verifier is cleared
# (This requires root or debugging build)
```

---

### Test 6: Session Persistence

**Steps:**
1. Complete OAuth flow successfully
2. Kill the app (swipe away from recents)
3. Relaunch the app
4. **Expected:** App shows repository list immediately (no re-auth)
5. Check "Logout" button appears in top-right
6. Tap "Logout"
7. **Expected:** Return to "Connect GitHub" screen

**Success Criteria:**
- ✅ Access token persists across app restarts
- ✅ Token is encrypted in SharedPreferences
- ✅ Logout clears token completely

---

### Test 7: State Tampering Protection (CSRF)

**Manual security test:**
```bash
# Start OAuth flow normally
# Note the state parameter from logs
# Then manually trigger callback with wrong state

adb shell am start -W -a android.intent.action.VIEW \
  -d "builder://oauth/callback?code=validcode&state=wrongstate"
```

**Expected:**
- ✅ App rejects the callback
- ✅ Error: "Invalid state parameter"
- ✅ No token is saved

---

### Test 8: Browser Not Available

**Steps:**
1. Disable all browser apps (if possible on test device)
2. Tap "Connect GitHub"
3. **Expected:** Error toast: "No app can handle this action"

**Success Criteria:**
- ✅ App doesn't crash
- ✅ Error is surfaced to user

---

### Test 9: Multiple Rapid Taps

**Steps:**
1. Tap "Connect GitHub" multiple times rapidly (5+ times)
2. **Expected:** Only one browser window opens
3. Complete authorization
4. **Expected:** App returns to authenticated state correctly

**Success Criteria:**
- ✅ No duplicate OAuth flows
- ✅ No race conditions
- ✅ Single token obtained

---

### Test 10: Authorization Timeout

**Steps:**
1. Tap "Connect GitHub"
2. Wait on GitHub authorization page for >60 seconds (don't tap anything)
3. **Expected:** App shows timeout error after 60 seconds
4. Return to app
5. **Expected:** Error message with "Retry" button

**Success Criteria:**
- ✅ Timeout occurs at 60 seconds
- ✅ Clear error message
- ✅ No hanging state

---

## Performance Benchmarks

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Time to browser open | <1 sec | From tap to browser visible |
| OAuth callback processing | <2 sec | From redirect to auth complete |
| Total auth time | <10 sec | From tap to repo list loaded |
| Token persistence | Instant | Relaunch should skip auth |

## Security Checklist

- [ ] Code verifier is 43+ characters, cryptographically random
- [ ] Code challenge uses SHA-256 (method=S256)
- [ ] State parameter is unique per request
- [ ] State is validated on callback (CSRF protection)
- [ ] Tokens stored in EncryptedSharedPreferences
- [ ] Temporary PKCE data cleared after token exchange
- [ ] No sensitive data logged (tokens, verifiers)
- [ ] Deep link only accepts builder:// scheme
- [ ] Redirect URI matches exactly in GitHub OAuth App settings

## Known Limitations

1. **No client secret**: This OAuth flow uses public client (no secret) as per mobile app best practices
2. **Token refresh**: Access tokens don't expire, but if they do, user must re-authenticate
3. **Browser dependency**: Requires a browser app to be installed

## Troubleshooting

### Issue: "Failed to get access token"
- **Cause:** GitHub OAuth app not configured correctly
- **Fix:** Verify CLIENT_ID and redirect URI in GitHub settings

### Issue: Deep link not working
- **Cause:** AndroidManifest deep link configuration
- **Fix:** Verify `builder://oauth/callback` is registered
- **Test:** `adb shell dumpsys package com.builder | grep -A 10 "filter"`

### Issue: "Invalid state parameter"
- **Cause:** State mismatch or race condition
- **Fix:** Clear app data and retry
- **Debug:** Check logs for saved state vs received state

### Issue: Browser doesn't open
- **Cause:** No browser app or intent filter issue
- **Fix:** Install Chrome or another browser
- **Debug:** Check logcat for "No Activity found to handle Intent"

## Comparing to Device Flow

| Aspect | Device Flow (Old) | Auth Code Flow (New) |
|--------|-------------------|----------------------|
| User steps | 5-7 | 2-3 |
| Manual input | Type 8-char code | None |
| Browser | Manual open | Auto-opens |
| Time to complete | 30-60 sec | 5-15 sec |
| Mobile-optimized | ❌ | ✅ |
| Industry standard | For TV/IoT | ✅ For mobile |

## Test Environment Setup

### Required GitHub OAuth App Settings
1. Go to: https://github.com/settings/developers
2. Select your OAuth App (or create new)
3. **Required Settings:**
   - Application name: "Builder - Mobile Orchestration"
   - Homepage URL: (your choice)
   - Authorization callback URL: `builder://oauth/callback`
   - **IMPORTANT:** URL must match exactly (case-sensitive)

### Logcat Filter Commands
```bash
# Watch OAuth flow
adb logcat -s GitHubOAuthManager:* MainActivity:*

# Watch all Builder logs
adb logcat -s Builder:* | grep -i oauth

# Clear logs before test
adb logcat -c
```

## Success Metrics

After testing, the implementation is successful if:
- ✅ All 10 test cases pass
- ✅ Performance benchmarks met
- ✅ Security checklist complete
- ✅ No crashes or ANRs
- ✅ User experience is smooth and intuitive

## Reporting Issues

If you encounter issues:
1. Note the test case number
2. Capture logcat output
3. Note device/emulator details
4. Take screenshots of error states
5. Document steps to reproduce
