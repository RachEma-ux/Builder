#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

echo "========================================"
echo " Builder – full repair + build pipeline "
echo "========================================"
echo "Working dir: $(pwd)"
echo

# 0) Sanity check
if [ ! -f "settings.gradle.kts" ]; then
  echo "ERROR: Run this script from the Builder repo root"
  exit 1
fi

# 1) Stop Gradle
echo ">> Stopping Gradle daemons"
./gradlew --stop || true
echo

# 2) Clean Gradle caches (safe)
echo ">> Cleaning Gradle caches"
rm -rf ~/.gradle/caches || true
rm -rf .gradle || true
echo

# 3) Ensure runtime uses Kotlin DSL only
if [ -f runtime/build.gradle ]; then
  echo ">> Removing accidental Groovy runtime/build.gradle"
  rm -f runtime/build.gradle
fi

if [ ! -f runtime/build.gradle.kts ]; then
  echo "ERROR: runtime/build.gradle.kts is missing"
  exit 1
fi
echo

# 4) Ensure runtime depends on :core
RUNTIME_GRADLE="runtime/build.gradle.kts"
if ! grep -q 'project(":core")' "$RUNTIME_GRADLE"; then
  echo ">> Adding :core dependency to runtime"
  cat >> "$RUNTIME_GRADLE" <<'EOF'

dependencies {
    implementation(project(":core"))
}
EOF
fi
echo

# 5) Guard: runtime must NOT import data / DAO
echo ">> Checking runtime for forbidden data/DAO imports"
if rg -n "com\.builder\.data|InstanceDao|@Dao|RoomDatabase" runtime/src/main/kotlin; then
  echo
  echo "ERROR: runtime module still references data/DAO code"
  exit 1
fi
echo "OK: runtime is clean"
echo

# 6) Compile runtime only (fast fail)
echo ">> Compiling :runtime only"
./gradlew :runtime:compileDebugKotlin --stacktrace
echo

# 7) Full build / install
if [ -f install.sh ]; then
  echo ">> Running full install"
  chmod +x install.sh
  ./install.sh
else
  echo ">> install.sh not found, running full build instead"
  ./gradlew build --stacktrace
fi

echo
echo "========================================"
echo " BUILD PIPELINE COMPLETED SUCCESSFULLY "
echo "========================================"
