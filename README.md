# tactos

**One app instead of fifty.** A privacy-first Android multi-tool built around a smart
clipboard — no ads, no accounts, no network, no nonsense.

[![CI](https://github.com/jackofall1232/tactos/actions/workflows/ci.yml/badge.svg)](https://github.com/jackofall1232/tactos/actions/workflows/ci.yml)

## Why

Every small task on Android seems to demand its own throwaway app — a QR generator here, a
JSON formatter there, each with its own ads, analytics, and account prompts. tactos replaces
that pile with one open-source app whose spine is a smart clipboard timeline: everything you
copy is kept, searchable, type-detected, and wired to contextual actions. Around that spine,
modular toolboxes will grow over time — but only what actually ships is described below.

## What ships today (v1)

The current release is the **Clipboard OS** core:

- **Smart clipboard timeline** — Room-backed history with case-insensitive substring
  search, pin, favorite, delete, and per-item categories. Consecutive duplicates are
  deduplicated by content hash. Pinned clips sort first. The live timeline shows the most
  recent 500 items; the full history stays searchable.
- **Type detection** — every clip is classified offline and deterministically as one of:
  URL, email, IPv4/IPv6 address, hex/rgb color code, JSON, phone number, or plain text.
  Detectors run in a fixed priority chain and are exhaustively unit-tested, including
  adversarial inputs (`999.1.1.1`, `#ggg`, `{"a":}`).
- **Contextual actions per type**:
  - URL → open in browser, share, generate a QR code (rendered offline with zxing-core —
    no network involved).
  - Color → live preview swatch plus HEX ⇄ RGB ⇄ HSL/HSV conversions with tap-to-copy.
  - JSON → validate, beautify, minify — copy the result or save it as a new clip.
  - Every clip → copy, share, pin.
- **Capture ladder** — three opt-in ways to get clips in, none requiring special
  permissions (see [How capture works](#how-capture-works)).
- **Sensitive clips are never stored** — content the source app flags as sensitive
  (password managers, Android 13+ `EXTRA_IS_SENSITIVE`) is skipped entirely.
- **Auto-cleanup** — configurable retention by age and/or maximum item count. Pinned and
  favorite clips are always spared.
- **Onboarding and settings** — a plain-language capture disclosure on first run; settings
  for the capture toggle and retention.
- **Material 3 / Material You** — dynamic color on Android 12+, a static teal/amber
  palette below that, light and dark throughout.

Minimum Android version: 8.0 (API 26).

## How capture works

This section doubles as the capture disclosure. It is written to be accurate, not
reassuring — read it and decide.

Since Android 10, apps in the background cannot read the clipboard. That is a good
platform rule, and tactos does not sneak around it. Instead, v1 offers three ways to
capture, all of them under your control:

1. **Share to tactos.** Select text in any app, hit share, pick tactos. The text lands in
   your timeline. Works everywhere, requires nothing.
2. **Foreground capture.** Whenever you open tactos, it reads whatever is currently on the
   clipboard and saves it (Android allows clipboard reads for the app in the foreground).
   So the habit is: copy things as you go, open tactos when you want them kept. This can
   be switched off in settings.
3. **Manual add.** Type or paste anything into the timeline yourself.

**What tactos does not do yet:** automatic background capture. A future version plans to
offer it via an Android accessibility service — the standard mechanism sideloaded
clipboard managers use — but it is **not in this release**. When it arrives, it will be
off by default, behind its own explicit disclosure screen and a settings toggle, will do
nothing except clipboard capture, and will only ship after being verified on real devices.
Declining it will never cost you any other feature.

**Sensitive content:** if the app you copied from marks the clip as sensitive — password
managers do this, and Android 13+ formalizes it as `EXTRA_IS_SENSITIVE` — tactos does not
store it. Not masked, not encrypted-but-kept: not stored.

Everything captured stays in the app's private database on your device. Nothing is
transmitted anywhere — as the next section shows, the app cannot transmit anything.

## Privacy

- The APK declares **zero permissions**. Not even `INTERNET` — the app is physically
  incapable of network traffic. Anyone can verify this by inspecting the merged manifest
  of a built APK (e.g. `aapt dump permissions app-debug.apk`, or Android Studio's APK
  Analyzer) or reading [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml).
- No ads. No analytics. No crash reporting SDKs. No trackers.
- No accounts — nothing to sign up for, nothing to log in to.
- Fully offline. All data lives in the app's private on-device database and never leaves
  it. Android backup is disabled (`allowBackup="false"`).
- Open source under the MIT license; the dependency list is short and auditable
  ([`gradle/libs.versions.toml`](gradle/libs.versions.toml)) — no Firebase, no Google Play
  Services requirement, no closed-source dependencies.

Future toolboxes that genuinely need the network (URL tools, bring-your-own-key AI) will
add it only for explicit user-initiated actions, and any permission change goes through a
mandatory human review gate before merge.

## Install

tactos is currently distributed as a sideloadable APK:

- **From CI:** every green run of the [CI workflow](https://github.com/jackofall1232/tactos/actions/workflows/ci.yml)
  uploads a debug APK artifact — download it from the run's Artifacts section.
- **From source:** see [Build from source](#build-from-source).
- **Signed release:** a tagged v0.1.0 release APK with a SHA-256 checksum is coming to
  [GitHub Releases](https://github.com/jackofall1232/tactos/releases).

To sideload, Android will ask you to allow installs from your browser or file manager
("Install unknown apps" in system settings). That prompt is Android's standard gate for
any app installed outside a store; since the tactos APK requests zero permissions, you can
verify after install that it has nothing granted.

## Build from source

Requirements: JDK 21, Android SDK (platform 36).

```sh
git clone https://github.com/jackofall1232/tactos.git
cd tactos
./gradlew assembleDebug     # APK: app/build/outputs/apk/debug/
./gradlew test              # unit tests (JVM + Robolectric)
./gradlew :app:lintDebug    # Android lint
```

These are the same three commands CI runs on every pull request.

## Architecture

Gradle multi-module, Kotlin 2.x, Jetpack Compose. Each future toolbox becomes its own
`feature/` module behind a shared contract:

| Module | Role |
|---|---|
| `app/` | Shell: single-activity Compose navigation, home grid, settings, onboarding, module registry wiring |
| `core/model/` | Shared contracts: `ClipItem`, `ClipType`, `ToolboxModule`, `ClipAction`, `ModuleRegistry` |
| `core/detect/` | Pure-Kotlin content-type detectors (URL, email, IP, color, JSON, phone) |
| `core/actions/` | Pure action logic behind the contextual actions (color conversion, JSON transforms, QR encoding) |
| `core/database/` | Room schema, DAO, repository: timeline persistence, dedup, search, retention |
| `core/design/` | Theme (dynamic color + static fallback) and shared Compose components |
| `core/clipboard/` | Capture: the ladder rungs, sensitive-clip filtering, ingestion into the timeline |
| `feature/clipboard/` | The v1 toolbox: timeline UI, detail sheet, contextual-action surface |

Toolboxes plug in through the `ToolboxModule`/`ClipAction` contracts: a module declares its
identity and the actions it contributes for each clip type as plain data, and the app layer
binds execution. That seam is deliberate — it is the future third-party plugin API. See
[`docs/overview.md`](docs/overview.md) for the full architecture overview.

## Website

Project site: <https://jackofall1232.github.io/tactos/> (goes live once GitHub Pages is
enabled for the repository).

## Roadmap

Beyond the v1 Clipboard OS, toolboxes land one phase at a time, each as its own module:
developer tools and everyday utilities → privacy tools → images → AI (bring-your-own API
keys — Anthropic, OpenAI, Gemini, xAI, Ollama, OpenRouter) → PDF and on-device OCR →
network → device toolboxes, then a published plugin SDK and Play Store preparation. The
detailed, ordered list lives in [`.l00prite/todos.md`](.l00prite/todos.md).

The longer-term V2 direction is an **AI workflow engine**: a universal voice/typed command
bar that turns natural language ("resize these five images to 1080 wide, convert to WebP,
strip metadata") into a visible, locally-executed workflow over the app's declared tools.
The model never acts on the device directly — it can only emit structured intents against
the fixed registry of actions the app exposes, destructive steps always pause for
confirmation, and models can be cloud (your keys) or fully local. None of this exists yet;
it is a design constraint on v1 (every capability is modeled as a declarative action
descriptor from day one) rather than a shipped feature.

## Contributing

Issues and PRs welcome. This codebase is largely built by AI agents operating under a
strict protocol — see [`CLAUDE.md`](CLAUDE.md) and [`AGENTS.md`](AGENTS.md) for the
requirements, verification rules, and the mandatory human review gates (manifest changes,
new dependencies, public contract changes). Human contributors follow the same gates: keep
changes small, verified, and inside the privacy posture.

## License

[MIT](LICENSE)
