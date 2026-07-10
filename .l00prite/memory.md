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

## Avoid
- Do not store random temporary notes, speculative ideas, or stale debugging output here.
