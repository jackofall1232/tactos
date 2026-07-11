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

- **Declarative-actions deviation (v1, intentional):** favorite, delete, and set-category
  remain plain UI callbacks — CLAUDE.md Section 3 scopes the universal ClipActions to
  copy/share/pin. They MUST gain descriptors before the V2 intent registry consumes the
  seam. The nine v1 action ids (clipboard.{copy,share,pin,url.open,url.qr,color.convert,
  json.validate,json.beautify,json.minify}) are stable API, enforced by ClipboardToolboxTest.
- Sensitive clips are skip-always (never stored, no masked variant) — decided for v1;
  capture-on-focus defaults OFF everywhere (onboarding toggle + stored default) because
  every capture rung is opt-in.
- Retention is enforced at launch, on settings change, and after every save (capture store
  wrapper + ClipboardScreen afterSave hook); WorkManager deliberately not added (gate).
- The website has one hard rule: zero external requests (no CDN/fonts/JS/analytics) —
  it is the privacy posture made visible; enforced in website/README.md.

## Facts
- `ClipDescription.get/setExtras` are **API 24+** (verified against android-7.0.0_r1
  framework source). Only the `EXTRA_IS_SENSITIVE` constant is API 33; SensitiveClips
  uses the literal key. Sensitive-clip tests run Robolectric `sdk = [26, 34]` to keep
  this machine-checked (a bot reviewer got this wrong on PR #2).
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

## Avoid
- Do not store random temporary notes, speculative ideas, or stale debugging output here.
- **Kotlin block comments NEST.** A literal like `text/*` inside a KDoc opens a nested
  comment and swallows the rest of the file (cost CI run #19). Never write `/*` inside
  comments; reword globs.
- Robolectric: `ClipData.newUri` queries the ContentResolver and throws for unregistered
  authorities — construct `ClipData(ClipDescription(...), ClipData.Item(uri))` directly
  in tests (cost CI run #25).
