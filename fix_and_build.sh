#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

cd ~/Builder

echo "[1/6] Ensure tools exist"
command -v rg >/dev/null 2>&1 || (echo "Installing ripgrep..." && pkg install -y ripgrep)
command -v sed >/dev/null 2>&1 || (echo "sed missing? (unexpected)" && exit 1)

echo "[2/6] Patch InstanceManager: replace instanceDao -> store"
IM="runtime/src/main/kotlin/com/builder/runtime/instance/InstanceManager.kt"
if [ ! -f "$IM" ]; then
  echo "ERROR: $IM not found"
  exit 1
fi

# Replace all instanceDao. with store.
# (Safe because instanceDao is supposed to be removed)
sed -i 's/\binstanceDao\./store./g' "$IM"

echo "[3/6] Verify no more instanceDao usage in runtime"
if rg -n "\binstanceDao\b" runtime/src/main/kotlin >/dev/null; then
  echo "ERROR: still found 'instanceDao' references:"
  rg -n "\binstanceDao\b" runtime/src/main/kotlin || true
  exit 1
fi
echo "OK: no instanceDao in runtime"

echo "[4/6] Run runtime compile and save log"
LOG="/data/data/com.termux/files/home/Builder/runtime_compile.log"
./gradlew :runtime:compileDebugKotlin --stacktrace --info --console=plain 2>&1 | tee "$LOG" || true

echo
echo "[5/6] Show real Kotlin errors (if any)"
# show the most useful lines
rg -n "^(e:|error:)|Unresolved reference|cannot find symbol|Compilation error" "$LOG" | head -n 200 || true

echo
echo "[6/6] Try full build"
./gradlew assembleDebug --stacktrace --console=plain

echo "DONE ✅"
