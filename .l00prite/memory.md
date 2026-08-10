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
- **Image toolbox decisions (2026-08-10 completion-plan run, see ADR-0004):**
  (1) All image output goes through SAF (`ACTION_CREATE_DOCUMENT` /
  `ACTION_OPEN_DOCUMENT_TREE`) — never MediaStore — so the manifest stays
  zero-permission on every API level (26–28 MediaStore writes would need
  `WRITE_EXTERNAL_STORAGE`). Originals are never modified. (2) No Coil: a handful of
  picked images is decoded with downsampled `BitmapFactory`; revisit only if grids grow.
  (3) `core/images` is pure JVM and zero-dependency like `core/detect` — locally testable
  in this sandbox. (4) Code adapted from T8RIN/ImageToolbox (Apache-2.0) keeps its Apache
  header + a NOTICE entry; ports are enumerated in `docs/adr/0004-imagetoolbox-ports.md`;
  never import its `core/ui`/`core/data`, filters (CeCILL/G'MIC), Firebase, or network
  code. (5) `androidx.exifinterface` approved with the completion plan (the plan's one
  new external dependency); AVIF (`avif-coder` native AAR) deliberately deferred.
- **Toolchain facts (2026-08 bump):** the 2026 Compose BOM no longer carries the
  upstream-frozen `material-icons-*` artifacts — `material-icons-core` is pinned
  explicitly (1.7.8). On the AGP 8.x line with compileSdk 36, androidx.core must stay
  ≤ 1.17.x (1.19.0 requires compileSdk 37 + AGP 9.1). AGP 9 was deliberately not taken
  (needs Gradle 9.5 wrapper + behavior changes); it is the next queued toolchain step.
- The GitHub Actions artifact blob store (`*.blob.core.windows.net`) is not reachable
  from this sandbox even though the Actions API is — CI therefore also prints the Room
  schema JSON into the build log (`Print Room schemas` step) so the baseline can be
  reconstructed and committed from logs.
- `Screen` navigation state is saveable via a string codec
  (`Screen.encode/decode/backTarget/depth` in `TactosApp.kt`) — system back, the up
  arrow, and transition direction all derive from the same two pure functions; keep them
  in sync via `ScreenCodecTest`.

## Avoid
- Do not store random temporary notes, speculative ideas, or stale debugging output here.
