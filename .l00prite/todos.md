# Prioritized TODOs

## Next
- [ ] Spike: verify AccessibilityService clipboard capture on an emulator (API 29+ and
      34+), record the working mechanism and its limits in `.l00prite/memory.md` before
      building the full capture feature. Then wire the settings toggle to it (the
      settings row ships as a disabled "planned" placeholder).
- [ ] HUMAN: enable GitHub Pages (repo Settings → Pages → Source: GitHub Actions) —
      `pages.yml` fails on main until then; site: https://jackofall1232.github.io/tactos/
- [ ] HUMAN (DoD): sideload the CI APK on API 26 and API 34+ devices and exercise the
      full v1 surface (capture rungs, search/pin/favorite/cleanup, contextual actions);
      record evidence in the ledger. Verify `EXTRA_IS_SENSITIVE` skip against a real
      password manager on an API 33+ image.
- [ ] HUMAN: review the capture-disclosure wording (still marked DRAFT in-app — gate).
- [ ] v0.1.0 release prep: tag from main, signed APK + SHA-256 checksum on GitHub
      Releases (signing/release scripting = review gate; the website download button
      already points at releases/latest).

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
- 2026-07-11 — v1 polish session (PR #2, merged as c5899f7): `core/actions` (color/JSON/QR
  pure logic), `core/clipboard` (share ingest, focus capture, sensitive skip, non-text
  clip filtering), v1 contextual actions via ClipAction descriptors + executor registry
  (URL open/QR w/ https normalization, color conversions, JSON tools), DataStore settings
  + retention config (enforced at launch and after every save), first-run onboarding with
  disclosure (capture opt-in, default OFF), share target + focus capture in MainActivity,
  adaptive launcher icon, README rewrite, docs/overview.md, zero-external-request website
  + Pages workflow. CI green at merge (runs 27/28); 12-agent adversarial review (8 findings
  fixed) + two bot reviews dispositioned (see reviews/github/pr2-bot-reviews-20260711.md).
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
- Room DAO tests will need Robolectric or instrumented tests — new dependency ⇒ human
  review gate before adding.
- Toolchain bump (AGP/Compose BOM/androidx to current stable) queued — bump, let CI
  verify, keep pins in `gradle/libs.versions.toml`.
