# Prioritized TODOs

## Next
> **2026-07-14 reconciliation note:** this file, `ledger.md`, `memory.md`, and
> `state.json` had gone stale — PR #2 (`c5899f7`, merged to `main`) shipped settings
> persistence, capture-ladder rungs 2–3, all v1 contextual actions, retention/sensitive-clip
> handling, real CI, and README/website, but the session that did it never updated `.l00prite/`
> memory. Verified against actual source (not the old ledger, not README prose) before writing
> this. See the ledger entry dated 2026-07-14 for the full audit trail. Treat this as a
> concrete instance of the "State Rot" failure mode in `failures.md` — every implementation
> loop must update memory before stopping, with no exceptions for merged PRs either.

- [ ] Accessibility-service capture (ladder rung 1): still fully unbuilt — verified zero
      shipped/compiled `AccessibilityService` implementation in any Gradle module's `src/`
      tree (the only `AccessibilityService` code in the repo is the illustrative, non-shipped
      snippet inside `docs/spikes/accessibility-capture-spike.md`, which lives outside every
      module's source set on purpose). CLAUDE.md requires the mechanism be
      spike-verified on a real device/emulator before the feature is built — this sandbox has
      no device access, so the spike itself must happen on the maintainer's hardware. A
      spike protocol + doc-only illustrative code now live in
      `docs/spikes/accessibility-capture-spike.md` (owner-approved scope: doc/spike-kit only,
      no manifest or app changes this round). Do not build the real service, wire the
      manifest `<service>`, or touch the disclosure/settings toggle until the spike is done
      and its results are recorded here.
- [ ] Once the spike confirms a working mechanism: wire the accessibility toggle,
      `AndroidManifest.xml` `<service>` declaration, and finalize disclosure copy — this is
      a manifest human-review-gate action, never autonomous.
- [ ] Human device-verification pass (API 26 and API 34+): capture ladder end-to-end,
      declining-accessibility-still-works, `EXTRA_IS_SENSITIVE` on an API 33+ image. Checklist
      ready at `docs/device-verification.md` — this cannot be done from this sandbox; it needs
      the maintainer on real hardware before the v1 Definition of Done can be declared met.
- [ ] Disclosure copy final sign-off: `DisclosureScreen.kt` is still marked DRAFT
      in-code by design; a reviewed/refined-wording proposal is at
      `docs/disclosure-review.md` awaiting maintainer approval (human review gate).
- [ ] Toolchain bump (AGP/Compose BOM/androidx to current stable) — still queued, unchanged
      from before; bump behind CI verification as usual.

## Later
Phase order for the full vision (one toolbox phase at a time; each is its own
`feature/<toolbox>` module registered via `ModuleRegistry`):
- [ ] Phase 2 — developer toolbox (JSON tools, regex tester, JWT decoder, UUID, hashes,
      Base64, Unix time, Markdown preview, SQL/YAML/XML formatters, diff, API tester
      (network only on explicit user action), color picker + palette generator,
      QR/barcode generator, MIME lookup), code-snippet detection as a new `ClipType` in
      `core/detect`, and utilities (unit converter, currency converter — rates fetched
      only on user action, time zones, age calculator, random picker/dice, stopwatch,
      calculator, flashlight, compass, level, sound meter, Morse).
- [ ] Phase 3 — privacy toolbox (password generator, TOTP, text encrypt/decrypt, secure
      notes, metadata remover, random generator).
- [ ] Phase 4 — image toolbox (resize, crop, compress, PNG⇄JPEG⇄WEBP⇄AVIF, batch convert,
      EXIF removal, blur faces + background remover (on-device ML — dependency review
      gate), optional AI upscale (BYO keys, needs Phase 5), watermark, rename,
      PDF-from-images) and image clips in the timeline (`ClipType.IMAGE`).
- [ ] Phase 5 — AI toolbox: BYO keys (Anthropic, OpenAI, Gemini, xAI/Grok, Ollama,
      OpenRouter), Keystore-backed key storage, AI clip actions (summarize — including
      summarize-page for URL clips, rewrite, translate, generate code; then AI OCR, image
      caption, screenshot analysis once image clips exist) — first `INTERNET` permission,
      so the manifest human review gate applies. URL shorten action lands here or Phase 7
      (user-initiated network either way).
- [ ] Phase 6 — PDF toolbox (merge, split, rotate, compress, extract images, sign,
      encrypt, password removal for PDFs the user owns) and on-device OCR (dependency
      review gate: on-device only), including clipboard OCR — extract text from image
      clips in the timeline.
- [ ] Phase 7 — network toolbox (ping, traceroute, DNS, whois, IP lookup, local port
      scan, subnet calculator, WiFi analyzer, speed test, MAC lookup — all network use
      user-initiated) + IP contextual actions on clips.
- [ ] Phase 8 — device toolbox (storage analyzer, duplicate/large-file finder, APK info,
      installed-app export, sensors, battery health).
- [ ] Phase 9 — plugin SDK: extract `ToolboxModule`/`ClipAction` into a published contract
      with docs so third parties can build modules.
- [ ] Phase 10 — Play Store preparation: prominent-disclosure flow for the accessibility
      service, listing assets, release signing, data-safety form.

## V2 — AI workflow engine ("the foreman") 🧰🎙️🤖
Owner's vision (recorded 2026-07-10): not a toolbox with 80 drawers — a toolbox with a
foreman. A universal command bar (tap mic → speak → review plan → run) that turns natural
language ("resize these five images to 1080 wide, convert to WebP, strip metadata, save to
a new folder") into a safe, visible, locally-executed workflow. The product becomes *a
private, AI-controlled utility layer for Android*.

Architecture principles (binding on v1 design so V2 stays reachable — see `memory.md`):
- [ ] The model NEVER acts on the device directly. It emits structured intents (e.g.
      `{"intent":"batch_convert_images","inputs":{...},"requires_confirmation":true}`)
      against a fixed registry of app-exposed tools — a model cannot invent capabilities;
      it can only call the tools the app exposes. The `ToolboxModule`/`ClipAction`
      descriptor seam is the substrate: every toolbox capability doubles as a declarative,
      model-invocable action.
- [ ] Pipeline: model interprets → app maps to approved internal actions → user sees
      exactly what will happen → app executes locally → result shown, with undo where
      possible.
- [ ] Three control modes: **Ask** (explain only, change nothing), **Assist** (build the
      workflow, require approval), **Hands-free** (auto-run low-risk actions only:
      formatting, QR, resize-selected). Destructive/privacy-sensitive actions ALWAYS
      pause for confirmation: deleting files, overwriting originals, scanning whole
      folders, decrypting protected content, sending/uploading anything, touching
      passwords or secure notes.
- [ ] Voice ≠ cloud: transcription via Android speech recognition, on-device models, or
      Whisper-compatible local models; command model via Claude/ChatGPT/Gemini/OpenRouter
      /Ollama or a local endpoint. User-configurable policy, e.g. transcription on-device,
      command model Claude, private operations local-model-only, cloud uploads never.
- [ ] Tool chaining: multi-step natural commands ("crop square, resize 1200×1200, convert
      to JPEG, write the clipboard caption to a text file beside it") compose the same
      structured actions into workflows.
- [ ] Same command surface from every entry point: voice, typed prompts, widgets, share
      sheet, quick settings tile, notification actions, Tasker/automation intents, and a
      future accessibility overlay — one intent schema underneath.
- [ ] Example commands to acceptance-test against: batch PNG→WebP a folder; compress an
      image under 500 KB "without making it look terrible"; extract just the tracking
      number from the clipboard; merge screenshots into one PDF; summarize the last ten
      clips; OCR a receipt into a note; strip metadata from selected photos; format JSON
      and explain what's wrong; QR from the clipboard URL.

## Done
- 2026-07-11 — PR #2 (`c5899f7`, merged): closed out the rest of v1 Section 3 in one
  push — `core/clipboard` (ClipboardCapture foreground-refresh + dedup, ShareIngest
  ACTION_SEND wired to the DB, SensitiveClips EXTRA_IS_SENSITIVE filtering at capture
  time); `core/actions` (ColorValue HEX⇄RGB⇄HSL/HSV both directions, JsonTools
  validate/beautify/minify via kotlinx-serialization-json, QrCode wrapping real
  zxing-core `Encoder`) with adversarial table-driven tests; `SettingsRepository`
  (DataStore-backed: onboarding-complete, capture-on-focus, retention days/max-items) +
  `RetentionCleanup` (applied at launch/capture, no WorkManager — see memory.md); real
  `OnboardingScreen`/`SettingsScreen`; `ClipboardToolbox` executor registry
  (`ClipActionExecutors` + `ActionEffect` sealed interface, side-effect-free — matches the
  V2 declarative-actions constraint) wired into `ClipDetailSheet`; zero-permission
  manifest gained exactly one `ACTION_SEND` intent-filter (share-to-tactos), otherwise
  unchanged. Also shipped: real CI (assembleDebug+test+lint, unchanged since — already
  done, not a pending item), accurate README (does not overclaim — correctly says
  accessibility auto-capture is not in this release), a real self-contained
  `website/index.html` + GitHub Pages workflow, `docs/overview.md`. 134 `@Test` methods
  total across modules at time of audit. **This entry backfills what the merging session
  never recorded — see the 2026-07-14 reconciliation note above and the ledger.**
- 2026-07-10 — Unit 6: `feature/clipboard` timeline UI (search, type/category chips,
  pinned-first list, favorite toggle, manual add via ContentDetector, detail sheet with
  copy/share/pin/favorite/delete + category editor); ClipboardToolbox = first contract
  consumer (gate #4 active; stable ids clipboard.copy/share/pin); Room kept encapsulated
  behind TactosDb.repository(). CI run #10 green (6eebf86). V2 vision recorded same
  session (62fd26a).
- 2026-07-10 — Unit 5: `core/design` TactosTheme (dynamic + static palettes, light/dark)
  + ToolboxCard; app shell with ModuleRegistry-driven home grid, settings skeleton,
  DRAFT capture-disclosure screen; ToolboxModule.emoji contract addition. CI run #6
  green (45268d3). Deferred gates recorded: navigation-compose, DataStore, UI test
  harness, disclosure final wording.
- 2026-07-10 — Unit 4: `core/database` Room timeline (entity/DAO/repository, consecutive
  dedup, Unicode search, retention sparing pinned/favorite), 13 Robolectric tests,
  CI-green (run 29127626654). Cleanup *scheduling* (WorkManager) still pending — new
  dependency ⇒ review gate; wire it with the app-integration unit.
- 2026-07-10 — Unit 3: `core/detect` — six detectors + ContentDetector priority chain,
  ~415 blind adversarial test assertions (local suite green; see ledger).
- 2026-07-10 — Unit 2: `core/model` contracts (ClipType, ClipItem + contentHashOf,
  ClipAction, ToolboxModule, ModuleRegistry), 15 tests, CI-green (run 29126121268).
- 2026-07-10 — Unit 1: real Gradle multi-module project (wrapper, catalog, :app Compose
  shell w/ zero-permission manifest, :core:model, :core:detect, real CI) — assembleDebug
  green in CI, debug APK artifact uploaded (run 29126121268).
- 2026-07-10 — l00prite Planning Mode scaffold: CLAUDE.md, AGENTS.md, `.l00prite/` memory
  + prompts, `.claude/`/`.codex/` mirrors, vendor adapters, large-tier Kotlin skeleton.

## Notes for the next session
- This remote sandbox cannot reach Google Maven / dl.google.com (network policy):
  verify JVM modules locally with `--configure-on-demand`; Android targets verify in CI.
- Robolectric is already an approved, in-catalog dependency (used by `core/database`,
  `core/clipboard`, `feature/clipboard` tests) — extending it to a module that has no
  tests yet (e.g. `app/`) is not a new-dependency review gate, just wiring an existing
  entry into that module's `build.gradle.kts`.
- zxing-core, kotlinx-serialization-json, and androidx-datastore-preferences are also
  already in the catalog and in active use (`core/actions`, `app/settings`) — they are
  not open dependency decisions anymore.
- Toolchain bump (AGP/Compose BOM/androidx to current stable) queued — bump, let CI
  verify, keep pins in `gradle/libs.versions.toml`.
- **Read `.l00prite/` memory with healthy skepticism against actual source** at the start
  of every session — this file went stale for 4 days of real work (see 2026-07-14 note)
  because a merging session skipped the memory-update step. Cross-check claims like
  "still needs X dependency" or "not yet wired" against a real grep/read before trusting
  them, especially after any PR merge.
