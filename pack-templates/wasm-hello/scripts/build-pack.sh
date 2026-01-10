#!/bin/bash
#
# Build script for WASM pack
# See Builder_Final.md Appendix C for contract
#

set -e

# Required environment variables
: "${PACK_VARIANT:?PACK_VARIANT is required}"
: "${TARGET:?TARGET is required}"
: "${PACK_VERSION:?PACK_VERSION is required}"
: "${GIT_SHA:?GIT_SHA is required}"
: "${OUT_DIR:=out}"

echo "Building pack: $PACK_VARIANT"
echo "Target: $TARGET"
echo "Version: $PACK_VERSION"
echo "Git SHA: $GIT_SHA"

# Create output directory
mkdir -p "$OUT_DIR"
mkdir -p "$OUT_DIR/temp"

# Build WASM module
echo "Building WASM module..."
cd src
cargo build --release --target wasm32-wasi
cd ..

# Copy WASM binary
cp target/wasm32-wasi/release/hello.wasm "$OUT_DIR/temp/main.wasm"

# Generate pack.json
cat > "$OUT_DIR/temp/pack.json" <<EOF
{
  "pack_version": "0.1",
  "id": "com.example.$PACK_VARIANT",
  "name": "Hello WASM",
  "version": "$PACK_VERSION",
  "type": "wasm",
  "entry": "main.wasm",
  "permissions": {
    "filesystem": {
      "read": ["assets/**"],
      "write": ["state/**"]
    },
    "network": {
      "connect": [],
      "listen_localhost": false
    }
  },
  "limits": {
    "memory_mb": 64,
    "cpu_ms_per_sec": 100
  },
  "required_env": [],
  "build": {
    "git_sha": "$GIT_SHA",
    "built_at": "$(date -Iseconds)",
    "target": "$TARGET"
  }
}
EOF

# Create pack zip with deterministic naming
PACK_NAME="pack-${PACK_VARIANT}-${TARGET}-${PACK_VERSION}.zip"
echo "Creating pack: $PACK_NAME"

cd "$OUT_DIR/temp"
zip -r "../$PACK_NAME" .
cd ../..

# Cleanup
rm -rf "$OUT_DIR/temp"

echo "Pack built successfully: $OUT_DIR/$PACK_NAME"
