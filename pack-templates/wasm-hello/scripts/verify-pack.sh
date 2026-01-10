#!/bin/bash
#
# Verification script for pack
# See Builder_Final.md Appendix C for requirements
#

set -e

OUT_DIR="${OUT_DIR:=out}"

echo "Verifying pack in $OUT_DIR..."

# Find pack zip file
PACK_ZIP=$(find "$OUT_DIR" -name "pack-*.zip" | head -1)

if [ -z "$PACK_ZIP" ]; then
    echo "ERROR: No pack-*.zip found in $OUT_DIR"
    exit 1
fi

echo "Found pack: $PACK_ZIP"

# Verify naming convention
FILENAME=$(basename "$PACK_ZIP")
if [[ ! "$FILENAME" =~ ^pack-[a-z0-9-]+-[a-z0-9-]+-.*\.zip$ ]]; then
    echo "ERROR: Pack filename does not match convention: $FILENAME"
    echo "Expected: pack-<variant>-<target>-<version>.zip"
    exit 1
fi

echo "✓ Filename follows naming convention"

# Extract pack to temp directory for verification
TEMP_DIR="$OUT_DIR/verify_temp"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

unzip -q "$PACK_ZIP" -d "$TEMP_DIR"

# Verify pack.json exists at root
if [ ! -f "$TEMP_DIR/pack.json" ]; then
    echo "ERROR: pack.json not found at zip root"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "✓ pack.json found at zip root"

# Verify pack.json is valid JSON and has required fields
if ! jq -e '.id, .name, .version, .type, .entry, .permissions, .limits, .build' "$TEMP_DIR/pack.json" > /dev/null; then
    echo "ERROR: pack.json missing required fields"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "✓ pack.json has required fields"

# Verify entry file exists
ENTRY=$(jq -r '.entry' "$TEMP_DIR/pack.json")
if [ ! -f "$TEMP_DIR/$ENTRY" ]; then
    echo "ERROR: Entry file not found: $ENTRY"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "✓ Entry file exists: $ENTRY"

# Verify no unsafe paths (absolute paths or parent references)
if unzip -l "$PACK_ZIP" | grep -E '(^/|\.\./)'; then
    echo "ERROR: Pack contains unsafe paths (absolute or parent references)"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo "✓ No unsafe paths detected"

# Cleanup
rm -rf "$TEMP_DIR"

echo "✅ Pack verification passed: $FILENAME"
