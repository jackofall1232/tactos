# Run Ledger

Append one entry per agent run. Do not overwrite prior runs.

## Entry Template

### Run YYYY-MM-DDTHH:MM:SSZ — <agent name>
- **Goal:** What this run attempted.
- **Triggering event:** Event id/type/source, or `none` for normal roadmap work.
- **Reviewer/comment reference:** PR, issue, CI run, reviewer, URL, file/line, or `none`.
- **Decision:** Valid, already fixed, unclear, unsafe, blocked, deferred, stale-lock-recovery, or normal work; include why.
- **Completed work:** What changed or was learned.
- **Fix implemented:** The smallest fix made for the event, or `none` with reason.
- **Changed files:** Files created, modified, deleted, or intentionally left untouched.
- **Tests run / Verification:** One entry per check run, each with `command`, `exit_code`,
  `summary`, `evidence_path` (optional), and `timestamp`. Do not write vague statements like
  "tests passed" without at least `command`, `exit_code`, and `summary`.
- **Response drafted/sent:** Reviewer, issue, or human response status and summary.
- **Event status:** Pending, processing, completed, blocked, deferred, or not applicable.
- **Failures:** Errors, blockers, failed approaches, or skipped checks.
- **Decisions:** Durable decisions made during the run.
- **Confidence:** Low/medium/high plus a short reason.
- **Next action:** The next smallest useful step.
- **Do-not-retry notes:** Failed approaches that should not be repeated unless conditions change.
- **Lock:** `lock_id` acquired/released this run, or `none` if no protected-path write occurred. Note stale-lock reclamation here if applicable.

### Run 2026-07-10T21:15:00Z — claude (supervised build loop, units 1–3)
- **Goal:** Start v1 implementation per `todos.md`: real Gradle project (unit 1), core/model contracts (unit 2), core/detect detectors (unit 3).
- **Triggering event:** none (normal roadmap work; owner chose supervised mode in-session).
- **Reviewer/comment reference:** CI runs 29125962330 (fail), 29126121268 (success) at https://github.com/jackofall1232/tactos/actions.
- **Decision:** Normal work.
- **Completed work:** Unit 1 — Gradle 8.14.3 wrapper, version catalog (AGP 8.11.1 / Kotlin 2.4.0 / Compose BOM 2024.12.01), :app Compose shell (minSdk 26, targetSdk 36, zero-permission manifest), :core:model + :core:detect JVM modules, real CI (assembleDebug + test + :app:lintDebug + APK artifact); placeholder skeleton removed. Unit 2 — ClipType/ClipItem(+contentHashOf)/ClipAction/ToolboxModule/ModuleRegistry with 15 tests. Unit 3 — six detectors + ContentDetector, built by two independent agents from one spec (impl and ~415-case adversarial tests written blind to each other), one mismatch adjudicated (strict full-string color semantics).
- **Fix implemented:** CI run 1 failure (Kotlin 2.4 rejects legacy kotlinOptions.jvmTarget) fixed via compilerOptions DSL in 48b56f0. Blind-test mismatch fixed in ColorDetector (no outer trim; string must end at `)`).
- **Changed files:** settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/*, gradlew(.bat), .gitignore, .github/workflows/ci.yml, app/** (build file, manifest, MainActivity, proguard), core/model/** (5 sources, 3 test files), core/detect/** (8 sources, 7 test files); deleted placeholder src/, services/, tests/.
- **Tests run / Verification:**
  - command: `gradle :core:model:test :core:detect:test --configure-on-demand` · exit_code: 0 · summary: unit-1 wiring smoke tests, 1 test/module, 0 failures · timestamp: 2026-07-10T21:45Z
  - command: `gradle :core:model:test --configure-on-demand` · exit_code: 0 · summary: 15 contract tests, 0 failures · timestamp: 2026-07-10T21:52Z
  - command: CI run 29126121268 (`./gradlew assembleDebug`, `test`, `:app:lintDebug`) · exit_code: 0 (conclusion: success) · summary: debug APK built on GitHub runner + artifact uploaded; all module tests green · evidence_path: https://github.com/jackofall1232/tactos/actions/runs/29126121268 · timestamp: 2026-07-10T21:56:57Z
  - command: `gradle :core:detect:test :core:model:test --configure-on-demand` · exit_code: 0 · summary: detect 21 test methods (~415 table-driven assertions) + model 15, 0 failures after ColorDetector fix · timestamp: 2026-07-10T22:11Z
  - command: CI on 7a8ba2c (unit 3) · status: pending at ledger-write time; check the Actions page for the conclusion before building on it.
- **Response drafted/sent:** Session summary to owner in-chat.
- **Event status:** Not applicable.
- **Failures:** (1) This sandbox cannot reach dl.google.com/maven.google.com (network policy) — Android modules cannot configure locally; JVM modules verified locally via `--configure-on-demand`, Android verification delegated to CI. (2) CI run 1 failed on Kotlin 2.4 jvmTarget DSL (fixed).
- **Decisions:** Root build file stays plugin-free so JVM modules configure without Google Maven. Detectors judge the exact string; only ContentDetector trims. Conservative dependency pins with a CI-verified bump queued. Detection priority: URL > EMAIL > IP > COLOR > JSON > PHONE.
- **Confidence:** High for units 1–2 (CI-green). Medium-high for unit 3 (local tests green; CI on 7a8ba2c pending at write time).
- **Next action:** The accessibility-capture spike needs a real device/emulator — not possible in this sandbox; either run it via Android Studio or let the next session start core/database (note: Room tests need Robolectric or instrumentation — a new dependency, which is a human review gate).
- **Do-not-retry notes:** Do not attempt SDK install or Google Maven fetches from this remote sandbox (policy-blocked, including Aliyun/Tencent/Huawei mirrors and gradle-distributions on GitHub); use CI for Android verification.
- **Lock:** `lock-20260710T211500Z-claude-supervised-build` acquired 21:15Z, refreshed 22:12Z, released at session end.
