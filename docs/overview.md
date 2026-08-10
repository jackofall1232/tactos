# tactos — architecture overview

Contributor-facing map of how the code is put together and why. For the product
requirements and definition of done, read [`CLAUDE.md`](../CLAUDE.md) sections 1–4. For
agent operating rules and durable build-loop memory (state, todos, ledger, constraints),
see [`AGENTS.md`](../AGENTS.md) and the [`.l00prite/`](../.l00prite/) directory.

## Module dependency map

Gradle multi-module, Kotlin DSL, version catalog in `gradle/libs.versions.toml`.
Dependencies point strictly downward; `core/model` is the only module everything shares.

```
                        ┌─────────────────────────────┐
                        │            app/              │  Android application
                        │  shell, nav, ModuleRegistry  │  (zero-permission manifest)
                        │  wiring, settings, capture   │
                        └──────────────┬──────────────┘
                                       │
                 ┌─────────────────────┼──────────────────────┐
                 ▼                     ▼                      ▼
     ┌───────────────────┐  ┌──────────────────┐  ┌──────────────────┐
     │ feature/clipboard │  │  core/clipboard  │  │   core/design    │  Android libs
     │ timeline UI,      │  │  capture ladder, │  │  theme, shared   │
     │ detail sheet,     │  │  sensitive-clip  │  │  components      │
     │ ClipboardToolbox  │  │  filtering       │  │                  │
     └───────┬───────────┘  └────────┬─────────┘  └──────────────────┘
             │                       │
             ▼                       ▼
     ┌───────────────────┐  ┌──────────────────┐
     │   core/database   │  │   core/detect    │      ┌──────────────────┐
     │ Room entity/DAO/  │  │ type detectors + │      │   core/actions   │  pure JVM
     │ repository        │  │ priority chain   │      │ color/JSON/QR    │
     └───────┬───────────┘  └────────┬─────────┘      │ action logic     │
             │                       │                └────────┬─────────┘
             └───────────┬───────────┘───────────────────────┬─┘
                         ▼                                   ▼
                    ┌─────────────────────────────────────────┐
                    │               core/model                │  pure JVM
                    │  ClipItem, ClipType, ClipAction,        │
                    │  ToolboxModule, ModuleRegistry          │
                    └─────────────────────────────────────────┘
```

Pure-JVM modules (`core/model`, `core/detect`, `core/actions`, `core/images`) have no
Android dependency at all. `core/database`, `core/clipboard`, `core/design`,
`feature/clipboard`, and `feature/images` are Android libraries; `app/` is the only
application module.

The image toolbox follows the same split (not drawn above to keep the diagram readable):
`core/images` is pure JVM — formats, resize math, target-size search, EXIF removal
presets (adapted from ImageToolbox, see `docs/adr/0004-imagetoolbox-ports.md`), and the
rename-pattern engine — while `feature/images` holds the Android engine
(decode/encode/EXIF-strip/SAF writes) and the Compose tool screens, and registers
`ImagesToolbox` through the same `ModuleRegistry` seam as the clipboard. Platform side-effects are bound
in the UI layers that own them: the clipboard-toolbox executors return pure `ActionEffect`
values that `feature/clipboard`'s detail sheet interprets (clipboard writes, share intents,
browser launches), while `app/` binds capture, settings, and navigation.

## Data flow of a clip

```
capture rung                  detect                    persist                 present
─────────────                ────────                  ─────────               ─────────
share-to-tactos ─┐
foreground read ─┼─ text ─▶ ContentDetector.detect() ─▶ ClipRepository.save() ─▶ timeline Flow
manual add      ─┘          (URL → EMAIL → IP →         (contentHashOf dedup     (pinned first,
                             COLOR → JSON → PHONE,       of consecutive dups,     newest next,
       sensitive?            first match wins,           Room upsert)             live window =
       └─▶ dropped           else TEXT)                                           500, search =
           before                                                                 full history)
           detection                                            │
                                                                ▼
                                              detail sheet: ClipActions filtered by
                                              ClipType → executed by app-layer bindings
```

Step by step:

1. **Capture.** One of the ladder rungs (below) produces raw text. Clips flagged sensitive
   by the source app (`ClipDescription.EXTRA_IS_SENSITIVE` on Android 13+, and the
   equivalent hints password managers set) are dropped at this boundary — they never reach
   detection or storage.
2. **Detect.** `ContentDetector` (in `core/detect`) trims the text and runs the fixed
   priority chain URL → EMAIL → IP_ADDRESS → COLOR → JSON → PHONE; first match wins,
   fallback is TEXT. Detection is deterministic, offline, side-effect free.
3. **Persist.** `ClipRepository.save()` upserts through the DAO with consecutive-duplicate
   dedup keyed on a content hash. The entity carries type, timestamps, pinned/favorite
   flags, category, and a Unicode-lowercased search column; LIKE queries are escaped.
   Retention (`deleteOlderThan`, `trimToNewest`) always spares pinned and favorite rows.
4. **Present.** The timeline observes a `Flow` limited to the newest 500 rows
   (`ClipRepository.DEFAULT_TIMELINE_LIMIT`); search hits the full table. The detail sheet
   asks the `ModuleRegistry` for every `ClipAction` whose `appliesTo` set contains the
   clip's type and renders them as buttons.

## ClipAction: descriptors, not callbacks

`ClipAction` (in `core/model`) is deliberately a **pure data descriptor** — id, label,
contributing module, applicable clip types — with no execute method:

- Modules *declare* capabilities: `ClipboardToolbox.clipActions()` returns descriptors
  like `clipboard.copy` / `clipboard.share` / `clipboard.pin` and the type-specific
  `clipboard.url.*` / `clipboard.color.*` / `clipboard.json.*` actions.
- An id-keyed **executor registry** (`ClipActionExecutors` in `feature/clipboard`) maps
  each stable action id to a pure function returning an `ActionEffect`; the UI interprets
  effects into platform work (launch a browser, invoke the share sheet, render a QR
  bitmap). `core/model` never touches Android, so the contract compiles anywhere and
  stays consumable by future out-of-process plugins. In v1 the timeline metadata
  operations (favorite, delete, set-category) remain plain UI callbacks — they gain
  descriptors before the V2 intent registry consumes this seam.
- Pure computation behind actions (color-space conversion, JSON validate/beautify/minify,
  QR matrix encoding) lives in `core/actions`, unit-testable on the JVM with no emulator.

**Why this indirection exists** (binding constraint, recorded in `.l00prite/memory.md` and
the V2 section of `.l00prite/todos.md`): the V2 vision is an AI workflow engine where a
model converts natural language into structured intents executed locally. That only works
if every capability in the app is modeled *as data* — a fixed registry of declared actions
the model can reference but never bypass. The model can only call tools the app exposes;
it cannot invent capabilities. `ClipAction` descriptors plus the app-side executor registry
are exactly that registry, seeded in v1. This is also why changes to the
`ToolboxModule`/`ClipAction` contracts are a mandatory human review gate now that
`feature/clipboard` consumes them (CLAUDE.md section 6, gate 4).

`ToolboxModule` is the other half of the seam: identity, title, emoji, home-grid order,
and contributed actions — UI-free, so the same interface can later be published as the
third-party plugin SDK (roadmap phase 9) without a rewrite.

## Capture ladder

Android 10+ forbids background clipboard reads (only the foreground app and the active
input method can read the clipboard). tactos does not work around this; it layers opt-in
rungs, each independent and each degradable:

| Rung | Mechanism | Status |
|---|---|---|
| Share-to-tactos | `ACTION_SEND` text share target | shipped |
| Foreground capture | read clipboard when tactos gains focus, save via the normal flow | shipped (toggle in settings) |
| Manual add | user-entered text through the add dialog | shipped |
| Accessibility capture | `AccessibilityService`-based automatic background capture | **not shipped** — planned |

The future accessibility rung is the only path to true automatic capture on a sideloaded
app. Design commitments already fixed: off by default; its own explicit disclosure screen
plus a settings toggle; the service does nothing except clipboard capture; the mechanism
must be spike-verified on real devices/emulators (API 29+ and 34+) with results recorded
in `.l00prite/memory.md` *before* the feature is built; the manifest service declaration is
a human review gate. Everything else in the app must keep working with it declined — that
is part of the v1 definition of done, not an aspiration.

Sensitive-clip policy applies to every rung: flagged content is not stored in any form.

## Privacy invariants (and where they're machine-checked)

The invariants are properties of the build, not promises in prose:

- **Zero permissions.** `app/src/main/AndroidManifest.xml` declares none — no `INTERNET`.
  Verifiable on any built APK via the merged manifest. Any permission, service, receiver,
  or foreground-service-type change is a mandatory human review gate.
- **No network-at-rest, no analytics, no accounts, no closed-source deps.** Enforced as
  evaluator checks in the agent loop against [`.l00prite/constraints.md`](../.l00prite/constraints.md)
  (which also carries the autonomous-edit denylist); the dependency list in
  `gradle/libs.versions.toml` is the audit surface, and every new entry is a review gate.
- **Sensitive clips never stored**; **backup disabled** (`allowBackup="false"`).
- Future network use (AI toolbox, URL tools) happens only inside explicit user-initiated
  actions, with BYO keys in Keystore-backed storage, never in the repo, logs, or exports.

## Testing strategy

- **Pure JVM modules** (`core/model`, `core/detect`, `core/actions`): plain JUnit on the
  JVM, runnable locally without an Android SDK. Detector tests are **table-driven** with
  adversarial cases (`999.1.1.1`, `#ggg`, `{"a":}`, near-miss URLs and phones) — the
  detectors are the "smart" in smart clipboard, so they carry the densest test suites
  (~430 assertions across the chain).
- **Android modules** (`core/database`, `feature/clipboard`): Robolectric on the JVM —
  DAO/repository behavior (dedup, escaped LIKE search, retention sparing pinned/favorite)
  and timeline filtering run in `./gradlew test` without an emulator.
- **CI** (`.github/workflows/ci.yml`): `assembleDebug` + `test` + `:app:lintDebug` on
  every PR, JDK 21, debug APK uploaded as an artifact. The sandbox agents build in cannot
  reach Google Maven, so Android targets are verified in CI while JVM modules verify
  locally — the evaluator records command, exit code, and summary for every unit in
  [`.l00prite/ledger.md`](../.l00prite/ledger.md).
- **Device verification** (definition of done): the APK must be exercised by a human on
  API 26 and API 34+ before v1 is declared complete, with evidence in the ledger.

## Where things are decided

- Requirements, architecture, review gates: [`CLAUDE.md`](../CLAUDE.md)
- Architecture decision records: [`docs/adr/`](adr/)
- Roadmap phases and the V2 AI-workflow-engine spec: [`.l00prite/todos.md`](../.l00prite/todos.md)
- Hard constraints and denylist: [`.l00prite/constraints.md`](../.l00prite/constraints.md)
- Build history and verification evidence: [`.l00prite/ledger.md`](../.l00prite/ledger.md)
