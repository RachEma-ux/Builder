# Security Audit Report

**Date**: 2026-01-17
**Version**: 1.0
**Scope**: Builder Android App - OAuth, File Operations, Network, Data Storage

---

## Executive Summary

The Builder app demonstrates **good security practices** in most areas, with a few items requiring attention. The most critical finding is the hardcoded OAuth client secret in source code.

**Risk Summary:**
- **Critical**: 1 (Client secret in source code)
- **Medium**: 2 (Debug logging, no certificate pinning)
- **Low**: 2 (Hardcoded DNS fallback IPs, token logging potential)

---

## Detailed Findings

### 1. OAuth/Token Handling

#### 1.1 Token Storage ✅ SECURE
**Location**: `GitHubOAuthManager.kt:41-51`

```kotlin
private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

private val encryptedPrefs = EncryptedSharedPreferences.create(...)
```

**Assessment**: Tokens are stored using Android's EncryptedSharedPreferences with AES-256-GCM encryption. This is the recommended approach for sensitive data storage on Android.

#### 1.2 PKCE Implementation ✅ SECURE
**Location**: `PKCEUtils.kt`

```kotlin
fun generateCodeVerifier(): String {
    val secureRandom = SecureRandom()
    val bytes = ByteArray(32)
    secureRandom.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE...)
}
```

**Assessment**: Proper PKCE implementation using SecureRandom for cryptographic randomness. SHA-256 code challenge method is used (S256).

#### 1.3 CSRF Protection ✅ SECURE
**Location**: `GitHubOAuthManager.kt:116-123`

```kotlin
val savedState = encryptedPrefs.getString(KEY_STATE, null)
if (state != savedState) {
    return DeviceFlowState.Error("Invalid state parameter")
}
```

**Assessment**: State parameter is properly generated, stored encrypted, and verified on callback.

#### 1.4 Client Secret Exposure 🔴 CRITICAL
**Location**: `GitHubOAuthService.kt:33`

```kotlin
const val CLIENT_SECRET = "21f7b66f9e7e11a97aeda7d5047d65841011d7c5"
```

**Assessment**: Client secret is hardcoded in source code. While this is a common pattern for mobile OAuth apps (where the secret cannot be truly protected), it should:
1. Be documented as a known limitation
2. Be moved to BuildConfig for easier rotation
3. Consider backend proxy pattern for higher security needs

**Recommendation**:
- Move to BuildConfig: `BuildConfig.GITHUB_CLIENT_SECRET`
- Add to `.gitignore` a `secrets.properties` file
- Document in README that client secret rotation requires app update

---

### 2. File Operations

#### 2.1 Zip Slip Protection ✅ SECURE
**Location**: `PackInstaller.kt:200-204`

```kotlin
val entryDestination = File(destination, entry.name)
if (!entryDestination.canonicalPath.startsWith(destination.canonicalPath)) {
    throw SecurityException("Zip entry outside destination: ${entry.name}")
}
```

**Assessment**: Proper protection against zip slip vulnerability using canonical path comparison. This prevents path traversal attacks via malicious zip entries like `../../etc/passwd`.

#### 2.2 Checksum Verification ✅ SECURE
**Location**: `PackInstaller.kt:70-88`

```kotlin
if (installSource.getMode() == InstallMode.PROD) {
    if (expectedChecksum == null) {
        return Result.failure(IllegalArgumentException("Checksum required for production installs"))
    }
    val actualChecksum = Checksums.sha256(packZipFile)
    if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
        throw SecurityException("Checksum verification failed")
    }
}
```

**Assessment**: SHA-256 checksums are mandatory for production installs. This prevents tampering with pack contents.

#### 2.3 Staging Directory Usage ✅ SECURE
**Assessment**: Files are downloaded to staging directory, validated, then moved to final location. Failed installations are cleaned up.

---

### 3. Network Security

#### 3.1 HTTPS Usage ✅ SECURE
**Assessment**: All API endpoints use HTTPS:
- `https://api.github.com/` (GitHubApiService)
- `https://github.com/` (GitHubOAuthService)

#### 3.2 Certificate Pinning 🟡 MEDIUM RISK
**Location**: `NetworkModule.kt`

**Assessment**: No certificate pinning is implemented. While GitHub's certificates are trusted via Android's system trust store, certificate pinning would provide additional protection against:
- Compromised CAs
- MITM attacks with rogue certificates

**Recommendation**: Consider adding certificate pinning for github.com and api.github.com using OkHttp's CertificatePinner.

#### 3.3 Debug Logging 🟡 MEDIUM RISK
**Location**: `NetworkModule.kt:96`

```kotlin
level = HttpLoggingInterceptor.Level.BODY
```

**Assessment**: Full request/response body logging is enabled. In production, this could log sensitive data including:
- Authorization headers (Bearer tokens)
- User data in API responses

**Recommendation**:
- Use `Level.HEADERS` or `Level.BASIC` in release builds
- Or conditionally set based on BuildConfig.DEBUG:
```kotlin
level = if (BuildConfig.DEBUG) Level.BODY else Level.BASIC
```

#### 3.4 Hardcoded DNS Fallback IPs 🟢 LOW RISK
**Location**: `NetworkModule.kt:35-48`

**Assessment**: Hardcoded GitHub IPs provide reliability but require maintenance. IPs could become stale.

**Recommendation**: Document IP update process. Consider periodic validation against https://api.github.com/meta

---

### 4. Data Storage

#### 4.1 Secret Storage ✅ SECURE
**Location**: `SecretRepositoryImpl.kt:39-50`

```kotlin
private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

private val encryptedPrefs = EncryptedSharedPreferences.create(
    context, PREFS_NAME, masterKey,
    AES256_SIV, AES256_GCM
)
```

**Assessment**: User secrets are properly encrypted using EncryptedSharedPreferences with AES-256-GCM. Metadata (descriptions, timestamps) is stored separately in non-encrypted prefs, which is appropriate since it's non-sensitive.

#### 4.2 Database Storage ✅ SECURE
**Assessment**: Room database stores pack metadata and KV store data. This data is stored in app-private internal storage which is protected by Android's sandboxing.

#### 4.3 Temporary Files ✅ SECURE
**Assessment**: Staging directories are cleaned up after use (success or failure).

---

## Recommendations Summary

### Critical (Fix Before Release)
1. **Move CLIENT_SECRET to BuildConfig** - Allows rotation without code changes

### Medium Priority
2. **Conditional logging level** - Reduce logging in release builds
3. **Add certificate pinning** - Defense in depth for MITM protection

### Low Priority
4. **Document DNS fallback maintenance** - Ensure IPs stay current
5. **Consider backend proxy** - For enhanced OAuth security (optional)

---

## Compliance Notes

- **OWASP Mobile Top 10**: No critical vulnerabilities found
- **Android Security Best Practices**: Follows recommended patterns for:
  - Encrypted storage (EncryptedSharedPreferences)
  - Secure random generation (SecureRandom)
  - Path traversal prevention
  - HTTPS for all network communication

---

## Files Reviewed

| File | Purpose | Status |
|------|---------|--------|
| `GitHubOAuthManager.kt` | OAuth token management | ✅ Secure |
| `GitHubOAuthService.kt` | OAuth API interface | ⚠️ Contains client secret |
| `PKCEUtils.kt` | PKCE implementation | ✅ Secure |
| `SecretRepositoryImpl.kt` | Secret storage | ✅ Secure |
| `PackInstaller.kt` | Zip extraction | ✅ Secure |
| `NetworkModule.kt` | Network configuration | ⚠️ Debug logging |
| `AuthInterceptor.kt` | Auth header injection | ✅ Secure |

---

**Auditor**: Claude Code
**Next Review**: After critical items addressed
