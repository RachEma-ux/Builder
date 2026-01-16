#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="${HOME}/Builder"

echo "[1/9] Go to repo: $ROOT"
cd "$ROOT"

echo "[2/9] Safety backups folder"
BK="${ROOT}/.backup_fix_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BK"

echo "[3/9] Remove accidental runtime/build.gradle if present (Kotlin DSL project uses build.gradle.kts)"
if [ -f runtime/build.gradle ]; then
  echo "  - Found runtime/build.gradle (wrong file). Backing up + removing."
  cp -a runtime/build.gradle "$BK/runtime.build.gradle"
  rm -f runtime/build.gradle
fi

echo "[4/9] Quick diagnostic: show where InstanceDao is referenced"
if command -v rg >/dev/null 2>&1; then
  rg -n "InstanceDao|import com\.builder\.data\.local\.db\.dao\.InstanceDao" runtime/src/main/kotlin || true
else
  grep -RIn "InstanceDao" runtime/src/main/kotlin || true
fi

echo "[5/9] Create core contract: core/src/main/kotlin/com/builder/core/instance/InstanceStore.kt"
mkdir -p core/src/main/kotlin/com/builder/core/instance
STORE_FILE="core/src/main/kotlin/com/builder/core/instance/InstanceStore.kt"
if [ -f "$STORE_FILE" ]; then
  echo "  - InstanceStore.kt already exists. Backing up."
  cp -a "$STORE_FILE" "$BK/InstanceStore.kt"
fi

cat > "$STORE_FILE" <<'KOT'
package com.builder.core.instance

/**
 * Persistence contract used by runtime.
 * This MUST NOT depend on Room or DAO types.
 *
 * NOTE:
 * - Keep these methods minimal.
 * - If compilation fails after this script, adjust the method signatures to match what InstanceManager actually needs.
 */
interface InstanceStore {
  /**
   * Temporary minimal surface.
   * Replace these with real domain-friendly methods once you check InstanceManager usage.
   */
  fun listIds(): List<String>
  fun saveId(id: String)
}
KOT

echo "[6/9] Create data implementation: data/src/main/kotlin/com/builder/data/local/instance/RoomInstanceStore.kt"
mkdir -p data/src/main/kotlin/com/builder/data/local/instance
IMPL_FILE="data/src/main/kotlin/com/builder/data/local/instance/RoomInstanceStore.kt"
if [ -f "$IMPL_FILE" ]; then
  echo "  - RoomInstanceStore.kt already exists. Backing up."
  cp -a "$IMPL_FILE" "$BK/RoomInstanceStore.kt"
fi

cat > "$IMPL_FILE" <<'KOT'
package com.builder.data.local.instance

import com.builder.core.instance.InstanceStore
import com.builder.data.local.db.dao.InstanceDao
import javax.inject.Inject

/**
 * Room-backed implementation living in :data.
 * Adjust mapping to match your InstanceDao methods and entity model.
 */
class RoomInstanceStore @Inject constructor(
  private val dao: InstanceDao
) : InstanceStore {

  override fun listIds(): List<String> {
    // TODO: map to your real DAO method(s).
    // Example (you MUST adapt):
    // return dao.getAll().map { it.id }
    return emptyList()
  }

  override fun saveId(id: String) {
    // TODO: map to your real DAO insert'ish method(s).
    // Example (you MUST adapt):
    // dao.insert(InstanceEntity(id = id))
  }
}
KOT

echo "[7/9] Create Hilt binding module in :data: data/src/main/kotlin/com/builder/data/di/InstanceStoreModule.kt"
mkdir -p data/src/main/kotlin/com/builder/data/di
BIND_FILE="data/src/main/kotlin/com/builder/data/di/InstanceStoreModule.kt"
if [ -f "$BIND_FILE" ]; then
  echo "  - InstanceStoreModule.kt already exists. Backing up."
  cp -a "$BIND_FILE" "$BK/InstanceStoreModule.kt"
fi

cat > "$BIND_FILE" <<'KOT'
package com.builder.data.di

import com.builder.core.instance.InstanceStore
import com.builder.data.local.instance.RoomInstanceStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InstanceStoreModule {

  @Binds
  @Singleton
  abstract fun bindInstanceStore(impl: RoomInstanceStore): InstanceStore
}
KOT

echo "[8/9] Patch InstanceManager to depend on InstanceStore (no direct DAO import)"
IM_FILE="runtime/src/main/kotlin/com/builder/runtime/instance/InstanceManager.kt"
if [ ! -f "$IM_FILE" ]; then
  echo "ERROR: Can't find $IM_FILE"
  exit 1
fi

cp -a "$IM_FILE" "$BK/InstanceManager.kt"

# 8a) Replace import of InstanceDao with InstanceStore import
# 8b) Replace constructor param type InstanceDao -> InstanceStore
# This is a conservative patch; if file formatting differs, you may need to edit manually afterwards.
perl -0777 -i -pe '
  s/import\s+com\.builder\.data\.local\.db\.dao\.InstanceDao\s*\n/import com.builder.core.instance.InstanceStore\n/g;
  s/(\()\s*([A-Za-z_][A-Za-z0-9_]*)\s*:\s*InstanceDao(\s*,)/$1$2: InstanceStore$3/g;
' "$IM_FILE"

# If InstanceDao import is still there, remove it explicitly
perl -i -ne '
  next if /^import\s+com\.builder\.data\.local\.db\.dao\.InstanceDao\s*$/;
  print;
' "$IM_FILE"

# Ensure InstanceStore import exists (add near top after package line if missing)
if ! grep -q "import com.builder.core.instance.InstanceStore" "$IM_FILE"; then
  perl -0777 -i -pe 's/(package\s+[^\n]+\n)/$1\nimport com.builder.core.instance.InstanceStore\n/ if $0 !~ /import com\.builder\.core\.instance\.InstanceStore/;' "$IM_FILE"
fi

echo "  - InstanceManager patched. Backup at: $BK/InstanceManager.kt"

echo "[9/9] Verify runtime has :core dependency (usually already). Add if missing."
RUNTIME_GRADLE="runtime/build.gradle.kts"
if [ -f "$RUNTIME_GRADLE" ]; then
  cp -a "$RUNTIME_GRADLE" "$BK/runtime.build.gradle.kts"
  if ! grep -q 'project(":core")' "$RUNTIME_GRADLE"; then
    echo "  - Adding implementation(project(\":core\")) to runtime/build.gradle.kts"
    # Insert inside dependencies { } block if possible; otherwise append a dependencies block.
    if grep -q '^dependencies\s*{' "$RUNTIME_GRADLE"; then
      perl -0777 -i -pe 's/dependencies\s*\{\n/dependencies {\n    implementation(project(":core"))\n/ if $0 !~ /implementation\(project\(":core"\)\)/;' "$RUNTIME_GRADLE"
    else
      cat >> "$RUNTIME_GRADLE" <<'KOT'

dependencies {
    implementation(project(":core"))
}
KOT
    fi
  else
    echo "  - runtime already depends on :core ✓"
  fi
else
  echo "WARN: runtime/build.gradle.kts not found (unexpected)."
fi

echo
echo "=== NEXT ==="
echo "Now open your DAO to map methods properly (this matters for compilation):"
echo "  sed -n '1,200p' data/src/main/kotlin/com/builder/data/local/db/dao/InstanceDao.kt"
echo
echo "Try compiling runtime first:"
echo "  ./gradlew :runtime:compileDebugKotlin --stacktrace"
echo
echo "Then try the full install:"
echo "  ./install.sh"
echo
echo "Backups saved in: $BK"
