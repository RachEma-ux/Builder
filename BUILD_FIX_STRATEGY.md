# Combined Fix Strategy for Builder Build Errors

## 🎯 Strategic Overview

**Priority:** Fix 2 critical blockers → Build succeeds → Optionally address 3 warnings

**Approach:** Understand root causes (Analysis 2) + Apply practical fixes (Analysis 3)

---

## 📋 Complete Issue Inventory

### **🔴 BLOCKERS (Must Fix)**
1. **Group 1:** HealthMonitor.kt type errors (3 errors - lines 53, 54, 160)
2. **Group 2:** WasmRuntime.kt missing PermissionEnforcer (1 error - line 14)

### **🟡 WARNINGS (Optional Cleanup)**
3. **Group 3:** Wasmtime native library not found (build succeeds without it)
4. **Group 4:** BuildConfig deprecation warning (future AGP compatibility)
5. **Group 5:** KAPT processor options not recognized (informational only)

---

## 🔧 Execution Plan

### **Phase 1: Investigation (Read-Only)**

**Step 1.1 - Examine HealthMonitor.kt**
```bash
# Goal: Understand what's causing type errors at lines 53, 54, 160
```
- Read file around lines 45-70 and line 155-165
- Look for patterns:
  - `mutableMapOf()` / `ConcurrentHashMap()` without `<K, V>` (line 53)
  - `Long` being assigned where `String` expected (lines 54, 160)

**Step 1.2 - Check PermissionEnforcer existence**
```bash
# Goal: Does this class exist? Where?
find . -name "*.kt" | xargs grep -l "class PermissionEnforcer"
grep -r "PermissionEnforcer" runtime/src/
```
- Verify if class exists
- Check its package location
- Note: RuntimeModule.kt imports it at line 6

**Step 1.3 - Check Instance ID type**
- Read `core/src/.../model/Instance.kt`
- Determine if `Instance.id` is `Long` or `String`
- This tells us which direction to fix the type mismatch

---

### **Phase 2: Fix Group 2 First (Simpler)**

**Why first?** Missing class reference is easier - either create it or fix import

**Fix Option A: PermissionEnforcer exists but wrong import**
```kotlin
// In WasmRuntime.kt line 14, fix import to match actual package
import com.builder.runtime.wasm.PermissionEnforcer  // or wherever it actually is
```

**Fix Option B: PermissionEnforcer doesn't exist - Create it**
```kotlin
// Create: runtime/src/main/kotlin/com/builder/runtime/wasm/PermissionEnforcer.kt
package com.builder.runtime.wasm

class PermissionEnforcer {
    // Minimal implementation based on how RuntimeModule uses it
}
```

**Fix Option C: Remove PermissionEnforcer dependency**
```kotlin
// If not needed, remove from WasmRuntime.kt and RuntimeModule.kt
// (Less likely since RuntimeModule explicitly provides it at line 35)
```

**Verification:**
```bash
./gradlew :runtime:compileDebugKotlin --stacktrace
# Should see WasmRuntime.kt compile successfully
```

---

### **Phase 3: Fix Group 1 (More Complex)**

**Fix 3.1 - Line 53: Type inference failure**

**Root cause:** Map/collection created without explicit types

**Look for patterns like:**
```kotlin
val something = mutableMapOf()           // BAD
val something = ConcurrentHashMap()      // BAD
val something = emptyMap()               // BAD
```

**Fix with explicit types:**
```kotlin
val something = mutableMapOf<String, Long>()        // GOOD
val something: MutableMap<String, Long> = mutableMapOf()  // GOOD
val something = ConcurrentHashMap<String, Long>()   // GOOD
```

**Fix 3.2 - Lines 54 & 160: Long vs String mismatch**

**Scenario A: Instance IDs are actually Long - change receiver to accept Long**
```kotlin
// Before
fun processInstanceId(id: String) { ... }
map.put(instanceId, someValue)  // where map expects <String, V>

// After
fun processInstanceId(id: Long) { ... }
map.put(instanceId, someValue)  // where map is <Long, V>
```

**Scenario B: Receiver must be String - convert Long to String**
```kotlin
// Before
map["instance"] = instance.id  // id is Long, map expects String values

// After
map["instance"] = instance.id.toString()
```

**Verification:**
```bash
./gradlew :runtime:compileDebugKotlin --stacktrace
./gradlew :runtime:compileReleaseKotlin --stacktrace
# Both should succeed
```

---

### **Phase 4: Full Verification**

```bash
# Step 1: Clean build runtime module
./gradlew :runtime:clean :runtime:build

# Step 2: Run lint
./gradlew :runtime:lintDebug

# Step 3: Full project build
./gradlew clean build -x test -x testDebugUnitTest -x testReleaseUnitTest

# Step 4: Trigger GitHub Actions
# (push to claude/check-branch-health-8MfTd branch)
```

**Expected result:** Build completes, APK artifacts generated

---

### **Phase 5: Optional Warning Cleanup (Post-Success)**

**Group 3 - Wasmtime (if WASM features needed):**
```bash
./scripts/build-wasmtime.sh
# Ensure jniLibs are in expected location
```

**Group 4 - BuildConfig deprecation:**
```gradle
// In gradle.properties - remove line 27:
// android.defaults.buildfeatures.buildconfig=true

// In module build.gradle.kts - add if needed:
android {
    buildFeatures {
        buildConfig = true  // only if BuildConfig is actually used
    }
}
```

**Group 5 - KAPT warnings:**
- Check Kotlin + AGP + Hilt version alignment
- Review kapt arguments in build files

---

## 🚨 Troubleshooting

**If fixes don't work:**
1. Clear Gradle cache: `./gradlew cleanBuildCache`
2. Clear local caches: `rm -rf ~/.gradle/caches/`
3. Check Kotlin version compatibility
4. Review recent git commits to see what changed

**If GitHub Actions still fails:**
- Verify the branch being built has the latest commits
- Check the workflow is using the correct branch ref

---

## 📊 Success Criteria

✅ **Minimum success:** Groups 1 & 2 fixed → Build completes
✅ **Full success:** All 5 groups addressed → Clean build with no warnings
✅ **Ultimate success:** APK downloaded and installs on device

---

## 🔄 Execution Order Summary

1. **Investigation Phase** → Read files, understand context (no changes)
2. **Fix Group 2** → PermissionEnforcer (simpler, test quickly)
3. **Fix Group 1** → HealthMonitor types (more complex)
4. **Verify** → Test at module level, then full project
5. **Cleanup** → Address warnings if desired
6. **Push & Test** → GitHub Actions final verification

**Next step:** Begin Phase 1 - Investigation (read files to understand exact errors)
