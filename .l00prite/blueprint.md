# Project Blueprint

## Mission
tactos is an open-source, privacy-first Android app (Kotlin + Jetpack Compose, Material
You) that replaces fifty single-purpose utility apps with one. The spine is a smart
clipboard timeline — unlimited searchable history with pins, favorites, categories, content
type detection (URL / IP / color / JSON / email / phone), and per-type contextual actions.
Around it: modular toolboxes (images, PDF, bring-your-own-key AI, developer, privacy,
network, device, utilities) and, later, a third-party plugin architecture. Distribution is
sideload-first, Play Store later. Philosophy, non-negotiable: no ads, no subscriptions, no
accounts, offline-first, optional AI on the user's own keys, privacy-first, MIT-licensed,
fast, Material You. Success for v1: a sideloaded APK whose clipboard is strictly better
than stock Android with zero unrequested network traffic. The authoritative, fully
detailed blueprint is `CLAUDE.md` (Sections 1–4); this file is its vendor-neutral summary.

## Architecture
Kotlin 2.x + Jetpack Compose (Material 3; dynamic color on Android 12+, static palette
fallback), Gradle Kotlin DSL + version catalog, minSdk 26, targetSdk 36, applicationId
`dev.tactos.app`. Room (timeline), DataStore (settings), kotlinx-serialization (JSON
tools), zxing-core (offline QR). No Firebase, no analytics, no closed-source deps.

Target Gradle module layout (each toolbox its own module behind a shared contract — the
seam that later hardens into the plugin API):
- `app/` — shell: navigation, home grid, settings, onboarding + capture disclosure,
  `ModuleRegistry`.
- `core/model/` — `ClipItem`, `ClipType`, `ToolboxModule`, `ClipAction` contracts.
- `core/database/` — Room schema/DAOs, retention & auto-cleanup worker.
- `core/detect/` — pure-Kotlin type detectors, exhaustively unit-tested.
- `core/design/` — theme + shared components.
- `core/clipboard/` — capture ladder, dedup, `EXTRA_IS_SENSITIVE` handling.
- `feature/clipboard/` — v1 toolbox: timeline UI + contextual actions.
- `feature/<toolbox>/` — later phases.

Clipboard capture ladder (Android 10+ forbids background clipboard reads): (1) opt-in
AccessibilityService behind an explicit disclosure — spike-verify the mechanism on-device
early and record the result in `memory.md`; (2) share-to-tactos + manual add; (3)
foreground reads on app focus. The app stays fully functional with (1) declined. Network
boundary: v1 declares no INTERNET permission; network only ever inside explicit
user-initiated tool actions in later phases; AI keys in Keystore-backed encrypted storage,
never in the repo.

The scaffolded placeholder skeleton maps: `src/clipboard|detect` → `core/*`,
`services/app-shell` → `app/`, `services/toolbox-clipboard` → `feature/clipboard/`,
`tests/*` → per-module `src/test/kotlin` + `app/src/androidTest`. Replacing it with the
real Gradle project is the first implementation unit.

## Requirements
v1 scope — Clipboard OS core + shell (full checklist with acceptance detail in `CLAUDE.md`
Section 3):
- [ ] Gradle multi-module project; `./gradlew assembleDebug` yields an installable APK.
- [ ] Material You app shell with `ModuleRegistry` home grid, settings, onboarding.
- [ ] `ToolboxModule`/`ClipAction` contracts, consumed by the clipboard toolbox.
- [ ] Room timeline: persist, search, pin, favorite, delete, categories, dedup.
- [ ] Capture ladder (accessibility opt-in + share-target + manual + foreground refresh).
- [ ] `core/detect` detectors with adversarial table-driven unit tests.
- [ ] v1 contextual actions: URL (open/share/QR), color (preview/convert), JSON
      (validate/beautify/minify), universal copy/share/pin.
- [ ] Auto-cleanup with pinned/favorite exemption; sensitive-clip handling.
- [ ] Privacy invariants: no INTERNET permission, no analytics, no accounts.
- [ ] CI: assemble + unit tests + lint on every PR.

Later phases (ordered in `todos.md`): developer toolbox, utilities, privacy, images, AI
(BYO keys), PDF, network, device, OCR, plugin SDK, Play Store preparation.

## Definition of Done
- [ ] CI green from a clean clone: `assembleDebug`, `test`, `lint`.
- [ ] APK exercised on device/emulator (API 26 and 34+): capture ladder, timeline
      features, detection, v1 actions — human-verified, evidence in the ledger.
- [ ] Fully functional with accessibility declined.
- [ ] Merged manifest requests no INTERNET permission.
- [ ] Every v1 requirement checked with verification evidence.
- [ ] README covers sideload install, capture disclosure, privacy posture.
- [ ] Maintainer has reviewed and merged to `main`.

## Non-Execution Boundary
This blueprint is guidance for later implementation loops. Scaffolding tools must not execute the project unless a human explicitly starts an implementation session.
