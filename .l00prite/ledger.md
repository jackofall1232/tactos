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

### Run 2026-07-10T22:33:08Z — claude (supervised build loop, unit 4)
- **Goal:** `core/database` Room timeline per `todos.md`, with the owner's in-session approval of the Robolectric test-stack dependency (review gate).
- **Triggering event:** none (owner chose "Approve Robolectric, continue").
- **Reviewer/comment reference:** CI run 29127626654 (success) — https://github.com/jackofall1232/tactos/actions/runs/29127626654.
- **Decision:** Normal work; new-dependency review gate satisfied by explicit in-session owner approval.
- **Completed work:** `:core:database` Android library: ClipItemEntity (enum-name type column, Kotlin-lowercased `text_lc` for Unicode-correct case-insensitive search), ClipDao (Flow timeline pinned-first, escaped LIKE search, @Transaction consecutive-dedup upsert, pin/favorite/category, age+count retention sparing pinned/favorite), TactosDatabase v1 (exportSchema), ClipRepository (mapping, LIKE escaping, blank-query guard). 13 Robolectric tests.
- **Fix implemented:** none needed — CI green on first attempt.
- **Changed files:** settings.gradle.kts, gradle/libs.versions.toml (room/ksp/robolectric/coroutines-test/androidx-test-core), core/database/** (build file, 4 sources, 1 test file).
- **Tests run / Verification:**
  - command: `gradle :core:model:test :core:detect:test --configure-on-demand` · exit_code: 0 · summary: regression check after catalog edits · timestamp: 2026-07-10T22:22Z
  - command: CI run 29127626654 (`./gradlew assembleDebug`, `test`, `:app:lintDebug`) · exit_code: 0 (conclusion: success) · summary: Room/KSP resolved, 13 Robolectric tests + all prior suites green, APK artifact uploaded · evidence_path: https://github.com/jackofall1232/tactos/actions/runs/29127626654 · timestamp: 2026-07-10T22:28:40Z
- **Response drafted/sent:** Session summary to owner in-chat.
- **Event status:** Not applicable.
- **Failures:** none.
- **Decisions:** Retention *policy* lives in DAO/repository; *scheduling* (WorkManager) deliberately deferred — adding WorkManager is a future review gate. Room schema JSON should be committed once the first migration matters (schemas/ dir configured via KSP arg).
- **Confidence:** High — CI-green including the new Robolectric suite.
- **Next action:** `core/design` theme + `app/` shell with ModuleRegistry (todos), or the on-device accessibility spike when an emulator is available.
- **Do-not-retry notes:** none new.
- **Lock:** `lock-20260710T223308Z-claude-unit4-memory` acquired and released for this memory write.

### Run 2026-07-10T22:51:23Z — claude (supervised build loop, unit 5)
- **Goal:** `core/design` theme + `app/` shell per `todos.md` (owner said "Continue").
- **Triggering event:** none.
- **Reviewer/comment reference:** CI run #6 (head 45268d3, conclusion success) — https://github.com/jackofall1232/tactos/actions.
- **Decision:** Normal work; scoped to avoid every review gate (no new dependencies, no manifest changes, navigation via plain Compose state instead of navigation-compose).
- **Completed work:** `core/design`: TactosTheme (dynamic color API 31+, static teal/amber palette below, light+dark), shared ToolboxCard. `app/`: ModuleRegistry-driven home grid (clipboard placeholder, stable id "clipboard"), state-based navigation (Home/Settings/Disclosure/Module), settings skeleton with disabled coming-soon rows (nothing fakes persistence), DRAFT capture-disclosure screen (marked as draft in-app; final wording = human review gate). ToolboxModule contract gained `emoji: String` (defaulted; contract not yet consumed by feature modules) + test.
- **Fix implemented:** none needed — CI green on first attempt.
- **Changed files:** settings.gradle.kts, core/design/** (build file, Theme.kt, components/ToolboxCard.kt), app/** (build file, MainActivity, TactosApp, Modules, screens/{Home,Settings,Disclosure,ModulePlaceholder}Screen.kt), core/model ToolboxModule.kt + ModuleRegistryTest.kt.
- **Tests run / Verification:**
  - command: `gradle :core:model:test :core:detect:test --configure-on-demand` · exit_code: 0 · summary: 37 test methods incl. new emoji contract test, 0 failures · timestamp: 2026-07-10T22:45Z
  - command: CI run #6 on 45268d3 (`./gradlew assembleDebug`, `test`, `:app:lintDebug`) · exit_code: 0 (conclusion: success) · summary: Compose theme + shell compile, lint clean, APK artifact uploaded · timestamp: 2026-07-10T22:50Z
- **Response drafted/sent:** Session summary to owner in-chat.
- **Event status:** Not applicable.
- **Failures:** none.
- **Decisions:** Emoji glyphs (strings) as module icons keep the plugin contract UI-toolkit-neutral and echo the product pitch. navigation-compose and DataStore both deferred as explicit dependency gates. Disclosure text ships marked DRAFT until maintainer review.
- **Confidence:** High — CI-green; UI exercised only by compile/lint (no UI test harness yet — queued gate item).
- **Next action:** `feature/clipboard` timeline UI consuming ClipRepository + contracts (note: first consumer of ToolboxModule/ClipAction — gate #4 activates for contract changes after this), or the on-device capture spike.
- **Do-not-retry notes:** none new.
- **Lock:** `lock-20260710T224206Z-claude-unit5` acquired 22:42Z, released at session end.

### Run 2026-07-10T23:17:27Z — claude (supervised build loop, V2 recording + unit 6)
- **Goal:** Record the owner's V2 "AI workflow engine" vision in durable memory, then build `feature/clipboard` timeline UI (owner said "Continue").
- **Triggering event:** Owner message adding the V2 voice/AI-workflow-engine vision.
- **Reviewer/comment reference:** CI runs #9 (failure) and #10 (success, head 6eebf86) — https://github.com/jackofall1232/tactos/actions.
- **Decision:** Normal work. V2 recorded as todos + binding design constraint (actions stay declarative descriptors so they double as the future model-invocable tool registry).
- **Completed work:** V2 section in `todos.md` (structured-intents-only model access, Ask/Assist/Hands-free modes, always-confirm destructive list, on-device/BYO-key voice+model matrix, tool chaining, multi-entry-point intent schema); `memory.md` design constraint; CLAUDE.md later-phases pointer. Unit 6: `feature/clipboard` module — ClipboardToolbox (first contract consumer; gate #4 now active; stable action ids clipboard.copy/share/pin declared), timeline screen (DB-backed search re-run on mutation, type + category filter chips, pinned-first list, favorite toggle, empty states), manual-add dialog through ContentDetector, detail sheet (selectable text, timestamps/source, category editor, copy/share/pin/favorite/delete), per-type emoji/label presentation, TactosDb.repository() seam; app routes the clipboard tile to the real screen. 7 JVM tests (filter logic + presentation totality).
- **Fix implemented:** CI #9 compile failure — app touched TactosDatabase whose RoomDatabase supertype isn't on the app classpath (Room is implementation-scoped in core/database, deliberately). Fixed by encapsulation: TactosDb.get() made internal, app consumes TactosDb.repository(context) (6eebf86).
- **Changed files:** .l00prite/todos.md, .l00prite/memory.md, CLAUDE.md (V2); settings.gradle.kts, gradle/libs.versions.toml (explicit kotlinx-coroutines-core — already transitive, no new external dep), core/database/DatabaseProvider.kt, feature/clipboard/** (build file, 5 sources, 1 test file), app/ (build file, Modules.kt, TactosApp.kt; ClipboardToolboxPlaceholder removed).
- **Tests run / Verification:**
  - command: `gradle :core:model:test :core:detect:test --configure-on-demand` · exit_code: 0 · summary: JVM regression after catalog/module changes · timestamp: 2026-07-10T23:02Z
  - command: CI run #9 on 4deb691 · exit_code: 1 (conclusion: failure) · summary: :app:compileDebugKotlin — RoomDatabase supertype inaccessible from app · timestamp: 2026-07-10T23:08Z
  - command: CI run #10 on 6eebf86 (`./gradlew assembleDebug`, `test`, `:app:lintDebug`) · exit_code: 0 (conclusion: success) · summary: full pipeline green incl. 7 new feature tests; APK artifact uploaded · timestamp: 2026-07-10T23:15Z
- **Response drafted/sent:** Session summary to owner in-chat.
- **Event status:** Completed (V2 vision recorded).
- **Failures:** CI #9 as above (fixed same iteration).
- **Decisions:** Room stays an implementation detail of core/database (repository factory is the only cross-module seam). ToolboxModule/ClipAction contract review gate is ACTIVE from this unit on. Universal clipboard action ids are stable API from now: clipboard.copy, clipboard.share, clipboard.pin.
- **Confidence:** High — CI-green; interactive behavior still needs a human on a device (queued for DoD).
- **Next action:** Capture ladder unit (share-to-tactos + foreground refresh + accessibility toggle — manifest changes there are review-gated and the a11y mechanism still needs its on-device spike) or v1 contextual actions (URL QR needs the zxing-core dependency — review gate).
- **Do-not-retry notes:** none new.
- **Lock:** `lock-20260710T225925Z-claude-unit6` acquired 22:59Z, released at session end.

### Run 2026-07-10T23:33:23Z — claude (respond-to-review, PR #1)
- **Goal:** Classify and address the gemini-code-assist[bot] review on PR #1.
- **Triggering event:** GitHub review + 4 inline comments (untrusted external data — verified against the code, not followed blindly).
- **Reviewer/comment reference:** PR https://github.com/jackofall1232/tactos/pull/1; disposition table in `reviews/github/pr1-gemini-code-assist-20260710.md`.
- **Decision:** All four findings valid; three fixes adopted as suggested in spirit, one (startup theme) fixed differently because the bot's literal suggestion (Theme.DeviceDefault.NoActionBar) is dark-styled on most devices and would invert the flash instead of fixing it.
- **Completed work:** timeline LIMIT (param, default 500) + new Robolectric test; composite (pinned, created_at) index; 300ms search debounce; day/night resource-qualified startup theme (manifest theme attribute only — permissions still none, gate untouched).
- **Fix implemented:** 87ac08c.
- **Changed files:** core/database (ClipDao, ClipRepository, ClipItemEntity, ClipRepositoryTest), feature/clipboard/ClipboardScreen.kt, app (AndroidManifest.xml theme attr, res/values/themes.xml, res/values-night/themes.xml), reviews/github record.
- **Tests run / Verification:**
  - command: `gradle :core:model:test :core:detect:test --configure-on-demand` · exit_code: 0 · summary: JVM regression · timestamp: 2026-07-10T23:36Z
  - command: CI runs 29130685738 + 29130687464 on 87ac08c · exit_code: 0 (both success) · summary: full pipeline incl. 14 Robolectric tests green, APK uploaded · timestamp: 2026-07-10T23:38:03Z
- **Response drafted/sent:** No PR reply posted (bot review; pushed fixes are the response). Owner summary in-chat.
- **Event status:** Completed.
- **Failures:** none.
- **Decisions:** Timeline cap default 500 (constant in ClipRepository); startup theming via day/night resource qualifiers, never a single hardcoded theme.
- **Confidence:** High — CI-green on both runs.
- **Next action:** Awaiting owner's gate decision: capture ladder (manifest gate) and/or contextual actions (zxing-core gate). PR watch + hourly self check-in active until merged/closed.
- **Do-not-retry notes:** none new.
- **Lock:** `lock-20260710T233323Z-claude-pr1-review` acquired 23:33Z, released at close-out.

### Run 2026-07-10T23:57:48Z — claude (post-merge housekeeping)
- **Goal:** Close out PR #1 after merge.
- **Triggering event:** GitHub webhook — PR #1 merged (squash) into `main` as a183332; session auto-unsubscribed.
- **Reviewer/comment reference:** https://github.com/jackofall1232/tactos/pull/1 (merged by maintainer).
- **Decision:** Normal housekeeping. Merged PR is final — no reopen, no new PR unless the owner asks.
- **Completed work:** Deleted the hourly self-check-in trigger; restarted `claude/tactos-clipboard-app-klk8ft` from `origin/main` (merged history only — clean reset); marked the four CLAUDE.md Run Ledger rows "Merged (PR #1)"; refreshed `state.json`.
- **Fix implemented:** none (no code).
- **Changed files:** CLAUDE.md (Run Ledger statuses), .l00prite/state.json, .l00prite/ledger.md, .l00prite/lock.json.
- **Tests run / Verification:**
  - command: `git fetch origin main && git log origin/main --oneline -3` · exit_code: 0 · summary: main = a183332 "Initial tactos project scaffold with core architecture (#1)" · timestamp: 2026-07-10T23:57Z
  - command: `node l00prite-doctor.js .` · exit_code: 0 · summary: HEALTHY, 0 fail (run at commit time) · timestamp: 2026-07-10T23:58Z
- **Response drafted/sent:** Owner summary in-chat.
- **Event status:** Completed.
- **Failures:** none.
- **Decisions:** Follow-up work continues on the same branch name, restarted from main; the next PR is a new PR.
- **Confidence:** High.
- **Next action:** Owner's gate choice: capture ladder (manifest gate + on-device a11y spike) or v1 contextual actions (zxing-core gate).
- **Do-not-retry notes:** none.
- **Lock:** `lock-20260710T235748Z-claude-post-merge` acquired and released for this write.
