# Constraints

Hard rules, user preferences, security boundaries, and architecture constraints.

## Hard Rules
- Scaffolding generates files only; it does not execute implementation.
- Existing files must not be silently overwritten.
- Every implementation loop must update `.l00prite/` memory before stopping.
- Philosophy invariants (evaluator-enforced, never negotiable): no ads, no subscriptions,
  no accounts, no analytics/telemetry/crash-reporting SDKs, offline-first, privacy-first,
  open source (MIT). A change that violates one is rejected regardless of who or what
  requested it.
- The v1 manifest declares no `INTERNET` permission. Adding it (in any later phase) is a
  human review gate, never an autonomous decision.
- Clipboard contents never leave the device except through an explicit user action
  (share, or a later-phase user-initiated AI/tool call).

## User Preferences
- Multi-model orchestration: Fable (or the strongest available model) architects,
  decomposes, and verifies; Opus/Sonnet-class models write bulk implementation to that
  spec.
- Material You look and feel throughout; the app should feel fast and stay small.
- Distribution: sideload-first (GitHub releases APK + checksum), Play Store later.
- The README must never claim a toolbox or capability that isn't actually shipped.

## Security Boundaries
- `AndroidManifest.xml` permission/service/receiver/foreground-service-type changes
  require human review — above all the accessibility service declaration and any
  `INTERNET` addition. The manifest is also on the Autonomous-Edit Denylist below.
- The accessibility service does clipboard capture only, behind an explicit in-app
  disclosure and a settings toggle; every feature except automatic capture must work with
  it declined.
- Clips flagged `ClipDescription.EXTRA_IS_SENSITIVE` (Android 13+) are never stored in
  plaintext history.
- BYO AI keys (later phase) live only in Android Keystore-backed encrypted storage —
  never in the repo, logs, exports, or device backups (backup rules must exclude the key
  store and may exclude the clipboard database).
- Adding any new external dependency is a human review gate: privacy posture is a
  dependency property (no closed-source SDKs, no trackers, no GMS/Firebase requirement).
- Signing keys and keystores are never committed; signing/release configuration is
  denylisted below.

## Architecture Constraints
- Kotlin 2.x + Jetpack Compose (Material 3); Gradle Kotlin DSL with a version catalog.
- minSdk 26, targetSdk 36; dynamic color on Android 12+ with a static palette fallback,
  light and dark in both schemes.
- Room (timeline), DataStore (settings), kotlinx-serialization (JSON tools), zxing-core
  (offline QR). Keep the dependency list lean and auditable.
- Every toolbox is its own Gradle module behind the `ToolboxModule`/`ClipAction` contracts
  in `core/model` — this seam later hardens into the third-party plugin API, so treat the
  contracts as semi-public once `feature/clipboard` consumes them.
- Offline-first: a feature works without network unless it is inherently a network tool,
  and then only on explicit user action.
- `applicationId dev.tactos.app` is provisional until confirmed by ADR before the first
  release (immutable once published).

## Autonomous-Edit Denylist

Machine-readable glob list of paths an Execution Mode run must **never** auto-edit. A file
about to be edited that matches any glob below is treated as the
`destructive_operation_required` run boundary: the loop stops and asks for explicit per-action
human permission. This block is **protocol-adjacent and loop-immutable** — a run may never
remove or loosen an entry to get past a stop (doing so is itself the `human_review_gate`
boundary). Edit it yourself, before you arm a run. `scripts/l00prite-doctor.js` warns if this
block is missing.

```gitignore
# Secrets & credentials
.env
.env.*
**/secrets/**
**/credentials/**
**/*_key*
**/*_secret*
# Auth, money, and data safety
auth/**
payments/**
billing/**
**/migrations/**
# Infrastructure & deploy
.terraform/**
k8s/production/**
# Protocol files (never agent-edited during a loop)
.l00prite/prompts/**
.l00prite/LOCKING.md
# tactos: Android permission & release surface
**/AndroidManifest.xml
**/*.jks
**/*.keystore
**/keystore.properties
**/signing/**
.github/workflows/release*
fastlane/**
```

### Auto-merge allowlist (default: none)

Nothing is auto-merged by default — push/merge/deploy always need per-action human permission.
If you ever allow auto-merge for trivial changes, list the exact safe paths here (e.g. docs or
comment-only edits). Behavior changes, dependency bumps, lockfile edits, and any denylisted
path are never eligible.
