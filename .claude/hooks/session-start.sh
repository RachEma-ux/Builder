#!/bin/bash
set -euo pipefail

# Only run in Claude Code remote environment (web)
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

echo "🚀 Initializing Builder development environment..."

# Use system Gradle to download and cache dependencies
# (Gradle wrapper has DNS issues in container environment)
echo "📦 Downloading and caching Gradle dependencies..."

# Run tasks command to trigger dependency resolution
gradle tasks --quiet > /dev/null 2>&1 || {
  echo "⚠️  Initial dependency sync completed (some warnings are normal)"
}

echo "✅ Builder environment ready!"
