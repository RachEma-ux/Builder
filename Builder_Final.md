# Builder (Final) — Simple Mobile Orchestrator (MVP+)

**GitHub Actions Builder · Non-Root Smartphone Runtime**

This document is the single source of truth for a system that provides a **Coolify/Render-like "build → deploy → run/manage"** experience, while remaining simple, deterministic, and realistic on non-root smartphones.

> **Core invariant:**  
> The phone is the **runtime + control UI**.  
> GitHub Actions is the **builder**.  
> The phone never builds from source.

---

## 0) Scope, guarantees, and non-goals

### What this app IS

A non-root smartphone orchestration app that:

- connects to GitHub (OAuth)
- triggers builds on GitHub Actions
- installs prebuilt **Packs** locally in user-space
- runs Packs via a constrained runtime
- manages instances (Start / Pause / Stop)
- exposes Logs and basic Health
- installs deterministically using strict naming + `packs.index.json`

### What this app IS NOT

- Docker
- Kubernetes
- a general PaaS with uptime guarantees
- a phone-side build system or CI runner

### Anti–promise-creep rule (non-negotiable)

**GitHub Actions may use any toolchain, but outputs must be WASM/workflow Packs (or remote connectors).**

Node, Python, LLMs, etc. may exist only inside the builder.  
The phone installs only mobile-compatible Packs.

---

## 1) Install modes (hard policy)

### A) Production install — Release only

**Production install = tag Release assets only.**

**Definition**
- Source ref is a **Git tag** (recommended: `vX.Y.Z`)
- Build publishes a **GitHub Release**
- Phone installs **only** from Release assets:
  - `packs.index.json` (mandatory)
  - `pack-<variant>-<target>-<version>.zip` (mandatory naming)
  - optional verification artifacts

**Why**
- deterministic selection
- immutable artifacts
- strongest auditability
- no artifact expiration risk

✅ **UX: Select tag → Install**

---

### B) Dev install — Workflow artifacts only

**Dev install = workflow artifacts only.**

**Definition**
- Source ref is a **branch** or **commit**
- Build uploads **workflow artifacts**
- Phone installs **only** from artifacts
- Never installs from Releases

**Why**
- fast iteration
- keeps unstable builds out of production
- artifacts are explicitly ephemeral

✅ **UX: Select branch → Build → Install (artifact)**

---

## 2) End-to-end flows

### Dev flow

1. User selects repo + branch/commit
2. App triggers `workflow_dispatch`
3. Actions builds + verifies + uploads artifacts
4. App downloads artifact and installs
5. App runs instance locally

### Production flow

1. User selects repo + tag `vX.Y.Z`
2. Actions builds + verifies + passes promotion gate
3. Actions publishes Release assets:
   - `pack-*.zip`
   - `packs.index.json`
   - verification files (see §9)
4. App installs **from Release assets only**

---

## 3) Supported runtimes (device-side)

### MVP+ runtimes

- **Workflow runtime** (built-in)
- **WASM runtime** (embedded)

### Explicitly optional later

- embedded Python runtime
- embedded Node runtime
- embedded local LLM runtime

These are not implied. They exist only if explicitly shipped.

---

## 4) Pack contract (artifact boundary)

### Pack contents

A Pack is a zip containing files at the zip root:

```
pack-<variant>-<target>-<version>.zip
  pack.json
  main.wasm           (if type=wasm)
  workflow.json       (if type=workflow)
  assets/...
```

### `pack.json` (minimum)

Must define:
- `id`, `name`, `version`
- `type` (`wasm` | `workflow`)
- `entry`
- `permissions`
- `limits`
- `build` metadata

Example (illustrative):

```json
{
  "pack_version": "0.1",
  "id": "com.example.guardrouter",
  "name": "Guard Router",
  "version": "v1.2.3",
  "type": "wasm",
  "entry": "main.wasm",
  "permissions": {
    "filesystem": { "read": ["assets/**"], "write": ["state/**", "cache/**"] },
    "network": { "connect": ["https://api.example.com"], "listen_localhost": true }
  },
  "limits": { "memory_mb": 128, "cpu_ms_per_sec": 150 },
  "required_env": ["API_KEY", "SERVICE_TOKEN"],
  "build": { "git_sha": "abc123...", "built_at": "2026-01-10T...", "target": "android-arm64" }
}
```

---

## 5) Mandatory naming convention (non-negotiable)

All build outputs **MUST** be named:

**`pack-<variant>-<target>-<version>.zip`**

- `<variant>`: logical pack name (e.g., `guardrouter`, `workflow-quicktest`)
- `<target>`: `android-arm64` | `android-universal`
- `<version>`: tag for prod, dev string for artifacts (e.g., `0.0.0-dev+abc123`)

Examples:
- `pack-guardrouter-android-arm64-v1.2.3.zip`
- `pack-guardrouter-android-universal-v1.2.3.zip`
- `pack-workflow-quicktest-android-universal-v1.2.3.zip`

**Reason:** This is what makes installs deterministic.

---

## 6) `packs.index.json` (production-only, mandatory)

Published as a Release asset.

**Purpose:**
- declare default variant
- map device → target
- map variant+target → asset name

Example:

```json
{
  "index_version": "1.0",
  "default_variant": "guardrouter",
  "targets": {
    "android": { "preferred": ["android-arm64", "android-universal"] }
  },
  "variants": {
    "guardrouter": {
      "name": "Guard Router",
      "type": "wasm",
      "targets": {
        "android-arm64": {
          "asset": "pack-guardrouter-android-arm64-{version}.zip"
        },
        "android-universal": {
          "asset": "pack-guardrouter-android-universal-{version}.zip"
        }
      }
    }
  }
}
```

**The app never guesses assets in Production.**

---

## 7) Secrets model

### Core rules

- Secrets are **never inside Packs**
- Secrets are **never committed**
- Packs may declare **required secret names only**

### Declaration (in `pack.json`)

```json
{
  "required_env": ["API_KEY", "SERVICE_TOKEN"]
}
```

### Storage

- Secrets are stored **device-local**
- Prefer OS secure storage (KeyStore / Keychain)
- Never exported or synced by default

### Injection

At instance start, secrets are injected as:
- environment variables (WASM host env / workflow context)

Secrets are **not visible** in logs or pack files.

### UI

- **Pack → Secrets panel**
- Shows required keys
- "Set / Update value" dialog
- Missing secrets **block Start** with a clear error

---

## 8) `workflow.json` schema

### Purpose

Defines a deterministic, mobile-safe workflow runtime.

### Minimal schema (v1)

```json
{
  "workflow_version": "1.0",
  "id": "example.workflow",
  "description": "Optional description",
  "steps": [
    {
      "id": "step1",
      "type": "http.request",
      "method": "POST",
      "url": "https://api.example.com",
      "body": { "foo": "bar" }
    },
    {
      "id": "step2",
      "type": "wasm.call",
      "function": "handle",
      "input_from": "step1.response"
    }
  ]
}
```

### Supported step types (MVP)

| Type | Description |
|------|-------------|
| `http.request` | Outbound HTTP (subject to permissions) |
| `wasm.call` | Call exported WASM function |
| `kv.put` | Write scoped state |
| `kv.get` | Read scoped state |
| `log` | Structured log |
| `sleep` | Delay (foreground-only) |
| `emit.event` | Emit instance/app events |

**Hard constraint:** No loops, no dynamic code, no shell execution.

---

## 9) Production verification mechanism

### Minimum required (MVP)

For Production installs, Releases **must** include:

- `pack-*.zip`
- `packs.index.json`
- `checksums.sha256`

### Verification flow

1. App downloads `checksums.sha256`
2. App verifies SHA-256 of selected `pack-*.zip`
3. If mismatch → **block install**

### Optional (future-ready)

If a `*.sig` / Sigstore bundle exists:
- App verifies signature
- Enforces trusted identity (strict mode)

This keeps MVP simple while leaving a clean upgrade path to cosign.

---

## 10) Target detection (explicit scope)

### MVP scope

**Android only**

Detection rule:
- ABI `arm64-v8a` → `android-arm64`
- otherwise → `android-universal`

### iOS

**Explicitly out of scope.**

If added later:
- extend `packs.index.json.targets`
- extend resolver by platform first, then ABI

---

## 11) Phone-side asset resolution (production)

1. Load `packs.index.json` from Release
2. Select variant (user or default)
3. Detect target
4. Resolve asset template
5. Verify checksum
6. Install Pack
7. Create instance

**No heuristics. No "latest".**

---

## 12) Instance lifecycle (simple by design)

**States:**
- **Stopped**
- **Running**
- **Paused**

**Auto behavior:**
- background → Paused
- memory pressure → Paused
- non-zero exit → Stopped (store reason)

**No auto-restart loops in MVP+.**

---

## 13) UI model (IBM-style sidebar)

Sidebar sections:
- **Runtimes → Overview**
- **Packs → Installed / GitHub / Local file**
- **Builds → Dev Runs / Production Releases**
- **Instances**
- **Logs**
- **Health**

---

# Appendix A — UX Invariant: Dev vs Production must be impossible to confuse

## A1) Hard UX invariant

The GitHub deployment experience **MUST** be split into two explicit tabs:
- **Dev (Branches)**
- **Production (Tags)**

No mixed list views. No "all refs" dropdown by default.

## A2) Tab guardrails

### Dev (Branches)
- Source of truth: **Workflow runs**
- Install source: **Artifacts only**
- Banner text (always visible):  
  **"DEV builds install from workflow artifacts only (temporary). Not for production."**
- Hard block: refuse installing Release assets from this tab.

### Production (Tags)
- Source of truth: **Tags + Releases**
- Install source: **Release assets only**
- Banner text (always visible):  
  **"PRODUCTION installs from tag Release assets only (stable + auditable)."**
- Hard block:
  - refuse installing artifacts from this tab
  - block install if `packs.index.json` missing:
    **"This tag is not production-ready: packs.index.json missing."**

## A3) Persistent mode badges

- Packs list shows **DEV** or **PROD** badge permanently
- Instances inherit the badge
- Show source line:
  - Dev: `Artifact Run #123`
  - Prod: `Release v1.2.3`

## A4) One-time safety confirmations

- First DEV install: "Dev installs are temporary and may break."
- Installing PROD over DEV of same Pack ID: confirm replacement + stop instances

---

# Appendix B — shadcn/ui Component Map (IBM-style sidebar) for "Packs → GitHub" page

This is a UI component map (not full code) that matches the IBM-style collapsible sidebar layout and implements the **Dev vs Production** invariant using **Tabs, Badge, Alert, Command list, Dialog**.

## B1) Page route + layout

**Route:** `/packs/github`

**Layout wrapper**
- `ResizablePanelGroup`
  - Left: `Sidebar` (collapsible)
  - Right: main content

**Main content shell**
- Header row:
  - Title: "Packs"
  - Breadcrumb: `Packs / GitHub`
  - Right actions: `Button` ("Refresh"), `Button` ("Disconnect")
- Content card:
  - `Card` + `CardHeader` + `CardContent`

## B2) Core navigation + state primitives

- `Tabs` (top-level):
  - Tab 1: **Dev (Branches)**
  - Tab 2: **Production (Tags)**
- Shared selections state:
  - Selected GitHub account/org
  - Selected repo
  - (Dev) selected branch/ref + workflow
  - (Prod) selected tag/release
- Shared "source badge" component:
  - `Badge variant="warning"` text "DEV"
  - `Badge variant="success"` text "PROD"

## B3) Repo selection (shared in both tabs)

**Component pattern:** `Popover + Command` (search-first)
- `Popover`
  - trigger: `Button` ("Select repo…")
  - content:
    - `Command`
      - `CommandInput` (search repos)
      - `CommandList`
        - `CommandGroup` (Repos)
        - `CommandItem` (repo rows)
- Optional: `Dialog` for GitHub OAuth connect / permission details
- `Alert` (inline) if token missing/expired:
  - "GitHub connection required" + `Button` ("Connect")

## B4) Dev (Branches) tab — component map

### Top banner (always visible)
- `Alert variant="warning"`
  - Title: "DEV mode"
  - Description: "Installs from workflow artifacts only (temporary). Not for production."

### Branch/ref picker
- `Popover + Command`
  - `CommandGroup` = Branches
  - `CommandItem` = branch
- Optional: commit SHA display as `Badge variant="outline"` (e.g., `abc123…`)

### Build trigger
- `Button` primary: "Build (workflow_dispatch)"
- `Dialog` (confirm):
  - "Trigger build for branch X?"
  - Actions: Cancel / Build

### Workflow run list
- `Table` or `DataTable` (if you use it)
  - Columns: Status, Run ID, Started, Duration, Actions
  - Status badge: `Badge` (Queued/Running/Success/Fail)
  - Actions:
    - `Button` ("View logs")
    - `Button` ("Install artifact") enabled only on Success

### Install from artifact (hard-guarded)
- `Dialog` (install confirm)
  - Shows:
    - Pack name + DEV badge
    - Artifact run id
    - Warning copy
  - Actions: Cancel / Install
- Hard rule: **No Release install controls appear in this tab.**

## B5) Production (Tags) tab — component map

### Top banner (always visible)
- `Alert variant="default"` (or "success" style)
  - Title: "PRODUCTION mode"
  - Description: "Installs from tag Release assets only (stable + auditable)."

### Tag selector
- `Popover + Command`
  - `CommandGroup` = Tags (vX.Y.Z)
  - `CommandItem` = tag
- `Card` section: "Release details"
  - Release notes preview: `ScrollArea` + `Markdown` renderer (if you have one)

### packs.index.json validation display
- `Alert variant="destructive"` if missing:
  - "This tag is not production-ready: packs.index.json missing."
  - Disable install button
- If present:
  - `Badge variant="success"` "Index OK"
  - Optional: show resolved default variant + target mapping in `DescriptionList`

### Asset list (derived from packs.index.json)
- `Table`:
  - Columns: Variant, Target, Asset name, Actions
  - Default variant row: show `Badge variant="outline"` "Default"
- Primary CTA:
  - `Button` "Install Production Pack"
  - `Dialog` (confirm):
    - Pack + PROD badge
    - Version tag
    - Asset filename resolved
    - Actions: Cancel / Install

### Hard-guarded rules
- No workflow artifact UI in this tab.
- If `packs.index.json` missing, install actions are disabled and the destructive alert is shown.

## B6) Cross-mode replacement dialogs

When installing PROD over an installed DEV pack with same `pack_id`:
- `Dialog`
  - Title: "Replace DEV with PROD?"
  - Body: "This will stop running instances."
  - Actions: Cancel / Replace

First-time DEV install:
- `Dialog`
  - Title: "DEV install warning"
  - Checkbox: "Don't show again for Dev installs"
  - Actions: Cancel / Continue

## B7) Sidebar placement (IBM-style)

Sidebar group: **Packs**
- `NavItem`: Installed
- `NavItem`: GitHub (this page)
  - Optional sub-items:
    - "Deploy — Dev (Branches)"
    - "Deploy — Production (Tags)"

## B8) Minimal component inventory (shadcn/ui)

- Navigation/Layout: `ResizablePanelGroup`, `Sheet` (mobile nav), `Separator`
- Containers: `Card`, `ScrollArea`
- Inputs: `Tabs`, `Popover`, `Command`, `CommandInput`, `CommandList`, `CommandGroup`, `CommandItem`
- Feedback: `Alert`, `Badge`, `Toast` (optional)
- Actions: `Button`, `DropdownMenu` (optional)
- Confirmation: `Dialog`, `DialogContent`, `DialogFooter`
- Data: `Table` (or your DataTable)

## B9) State machine hooks (UI-level)

You can drive this page with a small UI state machine:
- `GitHubDisconnected → GitHubConnected`
- `RepoSelected → (DevTab | ProdTab)`
- Dev:
  - `BranchSelected → Building → BuildSuccess/BuildFail`
  - `BuildSuccess → Installable`
- Prod:
  - `TagSelected → IndexMissing/IndexOK`
  - `IndexOK → Installable`

(Keep it UI-only; don't introduce platform-level complexity into MVP.)

---

# Appendix C — Repo-side builder contract

## Required scripts

- `scripts/build-pack.sh`
- `scripts/verify-pack.sh`

## Required workflow

- `.github/workflows/pack-build-matrix.yml`

### `scripts/build-pack.sh` contract

**Inputs (env):**
- `PACK_VARIANT` (required in matrix builds)
- `PACK_ID`, `PACK_NAME`, `PACK_TYPE` (`wasm|workflow`)
- `TARGET` (`android-arm64|android-universal`)
- `PACK_VERSION`
- `GIT_SHA`
- `OUT_DIR`

**Outputs (required) in `OUT_DIR`:**
- `pack.json`
- entry file (`main.wasm` or `workflow.json`)
- **mandatory** zip name: `pack-${PACK_VARIANT}-${TARGET}-${PACK_VERSION}.zip`

(Implementation is project-specific; ensure it produces the above.)

### `scripts/verify-pack.sh` requirements

Must fail the build if:
- the mandatory zip filename does not exist
- `pack.json` missing required keys
- zip is missing `pack.json` or entry file at the zip root
- zip contains unsafe paths

### GitHub Actions workflow requirements

**Must include:**
- **Matrix build**: variants × targets
- **Verify step**: lint manifest + validate zip content
- **Checksum generation**: `sha256sum pack-*.zip > checksums.sha256`
- **Promotion gate**: Release publish only if all matrix entries pass
- **Publishing rules**
  - Always upload workflow artifacts (dev)
  - On tags: publish Release assets:
    - `pack-*.zip` (deterministic names)
    - `packs.index.json` (mandatory)
    - `checksums.sha256` (mandatory)

---

# Appendix D — Security guarantees summary

| Layer | Guarantee | Mechanism |
|-------|-----------|-----------|
| **Build** | Reproducible | GitHub Actions matrix, deterministic naming |
| **Transport** | Integrity | SHA-256 checksums, HTTPS |
| **Install** | Verification | Checksum match required |
| **Secrets** | Isolation | Never in Packs, device-local storage only |
| **Runtime** | Sandboxed | WASM isolation, permission-gated network |
| **Workflow** | Constrained | No loops, no shell, no dynamic code |

**Future upgrade path:**
- cosign signatures for cryptographic identity verification
- Sigstore transparency log integration

---

# Final assessment

With these additions, this spec provides:

✅ Secure secrets handling  
✅ A fully defined workflow runtime  
✅ A credible supply-chain story for production  
✅ Zero violation of the "simple, non-root smartphone" constraint  

**This spec is complete, shippable, and defensible — without drifting into platform bloat.**
