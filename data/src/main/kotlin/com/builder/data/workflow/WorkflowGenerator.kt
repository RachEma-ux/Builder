package com.builder.data.workflow

import com.builder.core.repository.ProjectType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates GitHub Actions workflow YAML files for Builder deployment.
 * Supports multiple project types with appropriate build steps.
 */
@Singleton
class WorkflowGenerator @Inject constructor() {

    /**
     * Generates a complete builder-deploy.yml workflow for the given project type.
     */
    fun generate(
        projectType: ProjectType,
        packName: String,
        defaultVersion: String = "1.0.0"
    ): String {
        return when (projectType) {
            ProjectType.NODEJS -> generateNodeJsWorkflow(packName, defaultVersion)
            ProjectType.PYTHON -> generatePythonWorkflow(packName, defaultVersion)
            ProjectType.RUST -> generateRustWorkflow(packName, defaultVersion)
            ProjectType.GO -> generateGoWorkflow(packName, defaultVersion)
            ProjectType.KOTLIN_JVM, ProjectType.JAVA -> generateJvmWorkflow(packName, defaultVersion)
            ProjectType.WASM -> generateWasmWorkflow(packName, defaultVersion)
            ProjectType.STATIC -> generateStaticWorkflow(packName, defaultVersion)
            ProjectType.UNKNOWN -> generateGenericWorkflow(packName, defaultVersion)
        }
    }

    private fun generateNodeJsWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version (e.g., $defaultVersion)'
        required: true
        default: '$defaultVersion'
      run_app:
        description: 'Start app with public URL after deploy?'
        required: true
        default: 'yes'
        type: choice
        options:
          - 'yes'
          - 'no'
      duration:
        description: 'How long to keep app running (minutes)'
        required: true
        default: '15'
        type: choice
        options:
          - '5'
          - '10'
          - '15'
          - '30'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - name: Reset Gist status
        if: ${'$'}{{ vars.TUNNEL_GIST_ID != '' && secrets.GIST_TOKEN != '' }}
        run: |
          GIST_ID="${'$'}{{ vars.TUNNEL_GIST_ID }}"
          GIST_TOKEN="${'$'}{{ secrets.GIST_TOKEN }}"
          TIMESTAMP=${'$'}(date -u +%Y-%m-%dT%H:%M:%SZ)
          GIST_JSON=${'$'}(jq -n --arg rid "${'$'}{{ github.run_id }}" --arg repo "${'$'}{{ github.repository }}" --arg ts "${'$'}TIMESTAMP" \
            '{tunnel_url: null, status: "pending", run_id: ${'$'}rid, repository: ${'$'}repo, started_at: ${'$'}ts}')
          PAYLOAD=${'$'}(jq -n --arg content "${'$'}GIST_JSON" '{files: {"tunnel-status.json": {content: ${'$'}content}}}')
          curl -s -X PATCH "https://api.github.com/gists/${'$'}GIST_ID" \
            -H "Authorization: Bearer ${'$'}GIST_TOKEN" -H "Accept: application/vnd.github+json" -d "${'$'}PAYLOAD" > /dev/null

      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install dependencies
        run: |
          npm install --legacy-peer-deps || npm install

      - name: Build application
        run: |
          npm run build || echo "No build script found"
        env:
          NODE_OPTIONS: '--max-old-space-size=4096'

      - name: Create pack structure
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          PACK_NAME="$packName"
          PACK_DIR="pack-output"

          mkdir -p "${'$'}PACK_DIR"

          # Copy build output
          cp -r dist "${'$'}PACK_DIR/" 2>/dev/null || true
          cp -r build "${'$'}PACK_DIR/" 2>/dev/null || true
          cp package.json "${'$'}PACK_DIR/" 2>/dev/null || true

          # Create pack manifest
          BUILD_TIME=${'$'}(date -u +"%Y-%m-%dT%H:%M:%SZ")
          cat > "${'$'}PACK_DIR/pack.json" << EOF
          {
            "pack_version": 1,
            "id": "${'$'}PACK_NAME",
            "name": "${'$'}PACK_NAME",
            "version": "${'$'}VERSION",
            "type": "nodejs",
            "entry": "dist/index.js",
            "build": {
              "git_sha": "${'$'}{{ github.sha }}",
              "built_at": "${'$'}BUILD_TIME",
              "node_version": "20"
            }
          }
          EOF

      - name: Generate checksums
        run: |
          cd pack-output
          find . -type f ! -name "checksums.sha256" -exec sha256sum {} \; > checksums.sha256

      - name: Create pack archive
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          ARCHIVE_NAME="pack-$packName-android-arm64-v${'$'}{VERSION}.zip"
          cd pack-output && zip -r "../${'$'}ARCHIVE_NAME" .
          echo "ARCHIVE_NAME=${'$'}ARCHIVE_NAME" >> ${'$'}GITHUB_ENV

      - name: Delete existing release
        continue-on-error: true
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          git push origin --delete "v${'$'}{VERSION}" 2>/dev/null || true

      - name: Generate release checksum
        run: sha256sum "${'$'}{{ env.ARCHIVE_NAME }}" > checksums.sha256

      - name: Create GitHub Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          gh release create "v${'$'}{VERSION}" \
            --title "$packName v${'$'}{VERSION}" \
            --notes "Automated deployment from Builder" \
            "${'$'}{{ env.ARCHIVE_NAME }}" \
            "checksums.sha256"

      - name: Install cloudflared
        if: ${'$'}{{ github.event.inputs.run_app == 'yes' }}
        run: |
          curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
          chmod +x cloudflared

      - name: Start application with tunnel
        if: ${'$'}{{ github.event.inputs.run_app == 'yes' }}
        run: |
          PORT=3000 NODE_ENV=production node dist/index.js &
          sleep 10
          ./cloudflared tunnel --url http://localhost:3000 2>&1 | tee tunnel.log &
          sleep 15
          TUNNEL_URL=${'$'}(grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' tunnel.log | head -1)
          echo "URL: ${'$'}TUNNEL_URL"
          echo "${'$'}TUNNEL_URL" > app_url.txt

          # Update Gist with tunnel URL
          GIST_ID="${'$'}{{ vars.TUNNEL_GIST_ID }}"
          GIST_TOKEN="${'$'}{{ secrets.GIST_TOKEN }}"
          if [ -n "${'$'}GIST_TOKEN" ] && [ -n "${'$'}GIST_ID" ]; then
            TIMESTAMP=${'$'}(date -u +%Y-%m-%dT%H:%M:%SZ)
            GIST_JSON=${'$'}(jq -n --arg url "${'$'}TUNNEL_URL" --arg rid "${'$'}{{ github.run_id }}" --arg repo "${'$'}{{ github.repository }}" --arg ts "${'$'}TIMESTAMP" --arg dur "${'$'}{{ github.event.inputs.duration }}" \
              '{tunnel_url: ${'$'}url, status: "running", run_id: ${'$'}rid, repository: ${'$'}repo, started_at: ${'$'}ts, duration_minutes: (${'$'}dur|tonumber)}')
            PAYLOAD=${'$'}(jq -n --arg content "${'$'}GIST_JSON" '{files: {"tunnel-status.json": {content: ${'$'}content}}}')
            curl -s -X PATCH "https://api.github.com/gists/${'$'}GIST_ID" \
              -H "Authorization: Bearer ${'$'}GIST_TOKEN" -H "Accept: application/vnd.github+json" -d "${'$'}PAYLOAD" > /dev/null
          fi

          sleep ${'$'}(( ${'$'}{{ github.event.inputs.duration }} * 60 ))

      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: deployment-v${'$'}{{ github.event.inputs.version }}
          path: |
            app_url.txt
            pack-output/pack.json
          retention-days: 7
""".trimIndent()

    private fun generatePythonWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'
      run_app:
        description: 'Start app with public URL?'
        required: true
        default: 'yes'
        type: choice
        options:
          - 'yes'
          - 'no'
      duration:
        description: 'Runtime (minutes)'
        required: true
        default: '15'
        type: choice
        options:
          - '5'
          - '10'
          - '15'
          - '30'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    timeout-minutes: 45

    steps:
      - uses: actions/checkout@v4

      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: 'pip'

      - name: Install dependencies
        run: |
          pip install -r requirements.txt 2>/dev/null || pip install .

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          mkdir -p pack-output
          cp -r . pack-output/ 2>/dev/null || true
          cat > pack-output/pack.json << EOF
          {
            "pack_version": 1,
            "id": "$packName",
            "name": "$packName",
            "version": "${'$'}VERSION",
            "type": "python",
            "entry": "main.py",
            "build": {
              "git_sha": "${'$'}{{ github.sha }}",
              "built_at": "${'$'}(date -u +"%Y-%m-%dT%H:%M:%SZ")",
              "python_version": "3.11"
            }
          }
          EOF

      - name: Generate checksums
        run: cd pack-output && find . -type f ! -name "checksums.sha256" -exec sha256sum {} \; > checksums.sha256

      - name: Create archive
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          ARCHIVE_NAME="pack-$packName-android-arm64-v${'$'}{VERSION}.zip"
          cd pack-output && zip -r "../${'$'}ARCHIVE_NAME" .
          echo "ARCHIVE_NAME=${'$'}ARCHIVE_NAME" >> ${'$'}GITHUB_ENV

      - name: Create Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          sha256sum "${'$'}{{ env.ARCHIVE_NAME }}" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" --title "$packName v${'$'}{VERSION}" "${'$'}{{ env.ARCHIVE_NAME }}" checksums.sha256

      - name: Run with tunnel
        if: ${'$'}{{ github.event.inputs.run_app == 'yes' }}
        run: |
          curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared && chmod +x cloudflared
          python main.py &
          sleep 5
          ./cloudflared tunnel --url http://localhost:8000 2>&1 | tee tunnel.log &
          sleep 15
          grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' tunnel.log | head -1 > app_url.txt
          cat app_url.txt
          sleep ${'$'}(( ${'$'}{{ github.event.inputs.duration }} * 60 ))

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: deployment
          path: app_url.txt
          retention-days: 7
""".trimIndent()

    private fun generateRustWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Rust
        uses: dtolnay/rust-action@stable
        with:
          targets: wasm32-unknown-unknown

      - name: Build WASM
        run: |
          cargo build --release --target wasm32-unknown-unknown
          mkdir -p pack-output
          cp target/wasm32-unknown-unknown/release/*.wasm pack-output/ 2>/dev/null || true

      - name: Create pack manifest
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cat > pack-output/pack.json << EOF
          {
            "pack_version": 1,
            "id": "$packName",
            "name": "$packName",
            "version": "${'$'}VERSION",
            "type": "wasm",
            "entry": "$packName.wasm",
            "build": { "git_sha": "${'$'}{{ github.sha }}" }
          }
          EOF

      - name: Create release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" checksums.sha256
""".trimIndent()

    private fun generateGoWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'
      run_app:
        description: 'Start app?'
        required: true
        default: 'yes'
        type: choice
        options: ['yes', 'no']
      duration:
        description: 'Runtime (minutes)'
        required: true
        default: '15'
        type: choice
        options: ['5', '10', '15', '30']

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-go@v5
        with:
          go-version: '1.21'

      - name: Build
        run: |
          go build -o app .
          mkdir -p pack-output && cp app pack-output/

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cat > pack-output/pack.json << EOF
          {"pack_version":1,"id":"$packName","version":"${'$'}VERSION","type":"go","entry":"app"}
          EOF

      - name: Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" checksums.sha256

      - name: Run
        if: ${'$'}{{ github.event.inputs.run_app == 'yes' }}
        run: |
          curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared && chmod +x cloudflared
          ./pack-output/app &
          ./cloudflared tunnel --url http://localhost:8080 2>&1 | tee tunnel.log &
          sleep 15 && grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' tunnel.log
          sleep ${'$'}(( ${'$'}{{ github.event.inputs.duration }} * 60 ))
""".trimIndent()

    private fun generateJvmWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Build
        run: ./gradlew build || mvn package

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          mkdir -p pack-output
          cp build/libs/*.jar pack-output/ 2>/dev/null || cp target/*.jar pack-output/ 2>/dev/null || true
          cat > pack-output/pack.json << EOF
          {"pack_version":1,"id":"$packName","version":"${'$'}VERSION","type":"jvm","entry":"app.jar"}
          EOF

      - name: Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" checksums.sha256
""".trimIndent()

    private fun generateWasmWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          mkdir -p pack-output
          cp *.wasm pack-output/ 2>/dev/null || find . -name "*.wasm" -exec cp {} pack-output/ \;
          cp *.wat pack-output/ 2>/dev/null || true
          WASM_FILE=${'$'}(ls pack-output/*.wasm | head -1 | xargs basename)
          cat > pack-output/pack.json << EOF
          {"pack_version":1,"id":"$packName","version":"${'$'}VERSION","type":"wasm","entry":"${'$'}WASM_FILE"}
          EOF

      - name: Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && find . -type f ! -name "checksums.sha256" -exec sha256sum {} \; > checksums.sha256
          zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > release-checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" release-checksums.sha256
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
""".trimIndent()

    private fun generateStaticWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'
      run_app:
        description: 'Serve with public URL?'
        required: true
        default: 'yes'
        type: choice
        options: ['yes', 'no']
      duration:
        description: 'Runtime (minutes)'
        required: true
        default: '15'
        type: choice
        options: ['5', '10', '15', '30']

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          mkdir -p pack-output
          cp -r *.html *.css *.js assets/ static/ public/ pack-output/ 2>/dev/null || true
          cat > pack-output/pack.json << EOF
          {"pack_version":1,"id":"$packName","version":"${'$'}VERSION","type":"static","entry":"index.html"}
          EOF

      - name: Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" checksums.sha256

      - name: Serve
        if: ${'$'}{{ github.event.inputs.run_app == 'yes' }}
        run: |
          npm install -g serve
          curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared && chmod +x cloudflared
          serve -s pack-output -l 3000 &
          ./cloudflared tunnel --url http://localhost:3000 2>&1 | tee tunnel.log &
          sleep 15 && grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' tunnel.log
          sleep ${'$'}(( ${'$'}{{ github.event.inputs.duration }} * 60 ))
""".trimIndent()

    private fun generateGenericWorkflow(packName: String, defaultVersion: String): String = """
name: Builder Deploy

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version'
        required: true
        default: '$defaultVersion'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Create pack
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          mkdir -p pack-output
          # Copy all source files
          find . -maxdepth 2 -type f \( -name "*.js" -o -name "*.ts" -o -name "*.py" -o -name "*.go" -o -name "*.rs" -o -name "*.wasm" \) -exec cp {} pack-output/ \; 2>/dev/null || true
          cat > pack-output/pack.json << EOF
          {"pack_version":1,"id":"$packName","version":"${'$'}VERSION","type":"generic"}
          EOF

      - name: Release
        env:
          GH_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
        run: |
          VERSION="${'$'}{{ github.event.inputs.version }}"
          cd pack-output && zip -r "../pack-$packName-android-arm64-v${'$'}{VERSION}.zip" .
          cd .. && sha256sum "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" > checksums.sha256
          gh release delete "v${'$'}{VERSION}" --yes 2>/dev/null || true
          gh release create "v${'$'}{VERSION}" "pack-$packName-android-arm64-v${'$'}{VERSION}.zip" checksums.sha256
""".trimIndent()
}
