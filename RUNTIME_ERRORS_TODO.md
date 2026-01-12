# Runtime Errors Fixing - Todo List

This document tracks the step-by-step tasks to resolve the runtime module compilation errors identified in GitHub Actions build failures.

---

## 📋 Overview

**Total Tasks:** 18
**Critical Blockers:** 2 (HealthMonitor.kt + WasmRuntime.kt)
**Status:** Ready to begin Phase 1 - Investigation

---

## Phase 1: Investigation (4 tasks)

### ✅ Task 1: Read HealthMonitor.kt type inference error
**Status:** Pending
**Description:** Read HealthMonitor.kt lines 45-70 to understand line 53 type inference error
**Action:**
```bash
# Read the problematic section
cat -n runtime/src/main/kotlin/com/builder/runtime/HealthMonitor.kt | sed -n '45,70p'
```
**Expected outcome:** Identify the collection/map being created without explicit `<K, V>` type parameters

---

### ✅ Task 2: Read HealthMonitor.kt type mismatch errors
**Status:** Pending
**Description:** Read HealthMonitor.kt lines 155-165 to understand lines 54, 160 type mismatch
**Action:**
```bash
# Read the problematic section
cat -n runtime/src/main/kotlin/com/builder/runtime/HealthMonitor.kt | sed -n '50,60p'
cat -n runtime/src/main/kotlin/com/builder/runtime/HealthMonitor.kt | sed -n '155,165p'
```
**Expected outcome:** Identify where `Long` is being used where `String` is expected

---

### ✅ Task 3: Search for PermissionEnforcer class location
**Status:** Pending
**Description:** Search for PermissionEnforcer class location in codebase
**Action:**
```bash
# Search for the class
find . -name "*.kt" | xargs grep -l "class PermissionEnforcer"
grep -r "package.*PermissionEnforcer" runtime/src/
```
**Expected outcome:** Either find the class or confirm it needs to be created

---

### ✅ Task 4: Read Instance model to determine ID type
**Status:** Pending
**Description:** Read Instance model to determine if ID type is Long or String
**Action:**
```bash
# Find and read Instance model
find core/src -name "Instance.kt" -exec cat {} \;
```
**Expected outcome:** Confirm whether `Instance.id` is `Long` or `String`

---

## Phase 2: Fix Group 2 + Test (2 tasks)

### ✅ Task 5: Fix WasmRuntime.kt PermissionEnforcer reference
**Status:** Pending
**Description:** Fix WasmRuntime.kt line 14 - resolve PermissionEnforcer reference
**Action:**
- **Option A:** Fix import if class exists
- **Option B:** Create class if it doesn't exist
- **Option C:** Remove dependency if not needed

**Expected outcome:** WasmRuntime.kt imports PermissionEnforcer correctly

---

### ✅ Task 6: Test PermissionEnforcer fix with runtime compile
**Status:** Pending
**Description:** Test: Run ./gradlew :runtime:compileDebugKotlin to verify PermissionEnforcer fix
**Action:**
```bash
./gradlew :runtime:compileDebugKotlin --stacktrace
```
**Success criteria:** Compilation succeeds without "Unresolved reference: PermissionEnforcer" error

---

## Phase 3: Fix Group 1 + Test (5 tasks)

### ✅ Task 7: Fix HealthMonitor.kt line 53 type inference
**Status:** Pending
**Description:** Fix HealthMonitor.kt line 53 - add explicit type parameters to resolve inference error
**Action:**
- Change `mutableMapOf()` → `mutableMapOf<K, V>()`
- Or add explicit type declaration

**Expected outcome:** Line 53 no longer has "Type inference failed" error

---

### ✅ Task 8: Fix HealthMonitor.kt line 54 type mismatch
**Status:** Pending
**Description:** Fix HealthMonitor.kt line 54 - resolve Long vs String type mismatch
**Action:**
- **Scenario A:** Convert Long to String with `.toString()`
- **Scenario B:** Change receiver type to accept `Long`

**Expected outcome:** Line 54 no longer has "Type mismatch: inferred type is Long but String was expected"

---

### ✅ Task 9: Fix HealthMonitor.kt line 160 type mismatch
**Status:** Pending
**Description:** Fix HealthMonitor.kt line 160 - resolve Long vs String type mismatch
**Action:**
- **Scenario A:** Convert Long to String with `.toString()`
- **Scenario B:** Change receiver type to accept `Long`

**Expected outcome:** Line 160 no longer has type mismatch error

---

### ✅ Task 10: Test HealthMonitor fixes with debug compile
**Status:** Pending
**Description:** Test: Run ./gradlew :runtime:compileDebugKotlin to verify HealthMonitor fixes
**Action:**
```bash
./gradlew :runtime:compileDebugKotlin --stacktrace
```
**Success criteria:** No compilation errors in HealthMonitor.kt

---

### ✅ Task 11: Test release build compilation
**Status:** Pending
**Description:** Test: Run ./gradlew :runtime:compileReleaseKotlin to verify release build
**Action:**
```bash
./gradlew :runtime:compileReleaseKotlin --stacktrace
```
**Success criteria:** Both debug and release variants compile successfully

---

## Phase 4: Progressive Verification (4 tasks)

### ✅ Task 12: Test full runtime module build
**Status:** Pending
**Description:** Test: Run ./gradlew :runtime:build to verify full runtime module build
**Action:**
```bash
./gradlew :runtime:clean :runtime:build
```
**Success criteria:** Runtime module builds completely without errors

---

### ✅ Task 13: Run lint on runtime module
**Status:** Pending
**Description:** Test: Run ./gradlew :runtime:lintDebug to check for lint issues
**Action:**
```bash
./gradlew :runtime:lintDebug
```
**Success criteria:** Lint passes or only has warnings (no errors)

---

### ✅ Task 14: Test full project build
**Status:** Pending
**Description:** Test: Run ./gradlew build -x test to verify full project build
**Action:**
```bash
./gradlew clean build -x test -x testDebugUnitTest -x testReleaseUnitTest
```
**Success criteria:** Entire project builds successfully

---

### ✅ Task 15: Commit fixes to feature branch
**Status:** Pending
**Description:** Commit all fixes to claude/check-branch-health-8MfTd branch
**Action:**
```bash
git add -A
git commit -m "fix: Resolve runtime module compilation errors

- Fix HealthMonitor.kt type inference and type mismatch errors (lines 53, 54, 160)
- Fix WasmRuntime.kt missing PermissionEnforcer reference (line 14)
- All runtime compilation tests passing

Resolves GitHub Actions build failures"
```
**Expected outcome:** Changes committed with clear message

---

## Phase 5: CI Verification (3 tasks)

### ✅ Task 16: Push commits and trigger GitHub Actions
**Status:** Pending
**Description:** Push commits and trigger GitHub Actions build
**Action:**
```bash
git push -u origin claude/check-branch-health-8MfTd
```
**Expected outcome:** Changes pushed to remote, GitHub Actions workflow triggered automatically

---

### ✅ Task 17: Monitor GitHub Actions build
**Status:** Pending
**Description:** Test: Monitor GitHub Actions build for success
**Action:**
```bash
# In Termux
gh run list --repo RachEma-ux/Builder --workflow=android-ci.yml --branch=claude/check-branch-health-8MfTd --limit 1

# Watch the build
gh run watch <RUN_ID> --repo RachEma-ux/Builder
```
**Success criteria:** Build completes with ✓ status (not X)

---

### ✅ Task 18: Verify APK artifacts generation
**Status:** Pending
**Description:** Test: Verify APK artifacts are generated in GitHub Actions
**Action:**
```bash
# View artifacts
gh run view <RUN_ID> --repo RachEma-ux/Builder

# Download if successful
gh run download <RUN_ID> --repo RachEma-ux/Builder
```
**Success criteria:** Debug and/or Release APK artifacts are available for download

---

## 🎯 Success Criteria

- [ ] **Minimum:** Tasks 1-15 complete → Local build succeeds
- [ ] **Full:** Tasks 1-18 complete → GitHub Actions build succeeds
- [ ] **Ultimate:** APK downloaded and installs successfully on device

---

## 📝 Notes

- **Warnings not included:** Wasmtime, BuildConfig deprecation, KAPT options warnings
- **These can be addressed after build succeeds**
- **Focus:** Fix the 2 critical blockers first

---

## 🔗 Related Documents

- [BUILD_FIX_STRATEGY.md](./BUILD_FIX_STRATEGY.md) - Comprehensive strategy document
- [GitHub Actions Workflow](./.github/workflows/android-ci.yml) - CI configuration

---

**Last Updated:** 2026-01-12
**Status:** Ready to begin Phase 1
