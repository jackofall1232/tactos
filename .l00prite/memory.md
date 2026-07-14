# Project Memory

Durable project facts and decisions that future agents should preserve.

## Decisions
- Root `build.gradle.kts` stays free of plugin applications so pure-JVM modules
  (`core/model`, `core/detect`) can configure and test in environments without Google
  Maven access (`--configure-on-demand`). Do not add `apply false` plugin declarations
  to the root — they force AGP resolution at configuration time.
- Detectors judge the exact string they receive (strict full-string semantics); only
  `ContentDetector` trims input. Detection priority: URL > EMAIL > IP_ADDRESS > COLOR >
  JSON > PHONE > TEXT. Bare digit runs without `+` or separators are TEXT, not PHONE.
- `core/detect` stays zero-dependency (stdlib only, hand-rolled JSON validator) to keep
  the future plugin-API story clean and builds fast.
- Version catalog pins are conservative known-stable versions (AGP 8.11.1, Kotlin 2.4.0,
  Compose BOM 2024.12.01); bumps go through CI verification, never blind.
- The `:app` manifest ships with zero permissions in v1; `android:allowBackup="false"`.
- **Actions stay declarative (V2 groundwork).** The owner's V2 vision is an AI workflow
  engine that turns natural language into structured intents executed against
  app-exposed tools (see the V2 section in `todos.md`). Therefore every toolbox
  capability must be modeled as data (`ClipAction`-style descriptors + an app-side
  executor registry keyed by action id), never as ad-hoc UI callbacks — the same
  descriptors later become the model-invocable tool registry, with per-action
  risk levels (low-risk auto-runnable vs. always-confirm destructive/privacy-sensitive).

## Facts
- The Claude remote sandbox used for this repo cannot reach dl.google.com /
  maven.google.com (network policy, CONNECT 403) — including common mirrors and
  github.com/gradle/gradle-distributions. Maven Central, Gradle Plugin Portal, and
  services.gradle.org are reachable. Android builds verify in GitHub Actions CI
  (ubuntu-latest has the Android SDK preinstalled); JVM modules verify locally.
- CI (`.github/workflows/ci.yml`) runs assembleDebug + test + :app:lintDebug on pushes
  to `main`/`claude/**` and PRs, and uploads the debug APK as the `tactos-debug-apk`
  artifact.
- Kotlin 2.4 makes the legacy `kotlinOptions { jvmTarget = ... }` block a hard error —
  use the `kotlin { compilerOptions { } }` DSL.
- The accessibility-service capture spike (todos item 1) requires a real device or
  emulator; it has NOT been performed yet — the capture mechanism remains unverified.
- **PR #2 (`c5899f7`) merged to `main` on 2026-07-11 and closed out nearly all remaining
  v1 Section 3 scope** — see the 2026-07-14 `todos.md`/`ledger.md` reconciliation entries
  for the full verified inventory. The `.l00prite/` memory files were not updated by that
  session; this was caught and fixed on 2026-07-14 by auditing actual source against the
  CLAUDE.md Section 3 checklist rather than trusting stale ledger/todos claims. Lesson:
  the merge event itself is not evidence memory was updated — always verify.
- `core/actions` intentionally uses `kotlinx-serialization-json` for `JsonTools`
  (validate/beautify/minify), unlike `core/detect`'s deliberately zero-dependency
  hand-rolled JSON parser — these are two different modules with two different jobs
  (fast/dependency-free detection vs. full JSON manipulation) and this is not a
  contradiction of the `core/detect` decision above.
- Action executors are pure: `ClipActionExecutors` (in `feature/clipboard`) maps action
  ids to executor functions that return an `ActionEffect` sealed type (e.g. `CopyText`,
  `ShareText`, `ShowDialog`) with no side effects performed inside the registry itself —
  the UI layer interprets the effect. This directly satisfies the V2 binding constraint
  that every toolbox capability be modeled as declarative data, not ad-hoc callbacks.
- Retention (`RetentionCleanup`) runs at app launch and whenever rules change, not via a
  background `WorkManager` job — deliberate, since WorkManager would be a fresh
  new-dependency review gate. The DAO queries behind it already spare pinned/favorite
  clips, so this satisfies the Section 3 auto-cleanup requirement as worded.
- zxing-core, kotlinx-serialization-json, and androidx-datastore-preferences are already
  approved and in active use (added during PR #2) — they are no longer open
  new-dependency decisions. Robolectric is likewise already approved and used by
  `core/database`, `core/clipboard`, and `feature/clipboard` tests.
- `applicationId dev.tactos.app` was ratified by the maintainer on 2026-07-14 (see
  `docs/adr/0003-application-id.md`) — no longer provisional.

## Avoid
- Do not store random temporary notes, speculative ideas, or stale debugging output here.
