#!/bin/bash
# Build diagnostics script
# Run this to identify build issues

echo "=== Builder Build Diagnostics ==="
echo ""

echo "1. Checking module structure..."
./gradlew projects 2>&1 | grep -E "^\+" | head -20

echo ""
echo "2. Checking for circular dependencies..."
./gradlew :app:dependencies --configuration debugCompileClasspath 2>&1 | grep -i "circular\|cycle" | head -10

echo ""
echo "3. Checking Kapt annotation processors..."
./gradlew :app:kaptDebugKotlin --dry-run 2>&1 | grep -E "error|FAILED" | head -20

echo ""
echo "4. Trying to compile (this may take a minute)..."
./gradlew compileDebugKotlin 2>&1 | grep -E "^e:|error:|FAILURE" | head -30

echo ""
echo "=== END DIAGNOSTICS ==="
echo ""
echo "If you see errors above, copy them and share with me."
