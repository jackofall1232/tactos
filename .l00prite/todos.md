# Prioritized TODOs

## Next
- [ ] Spike: verify AccessibilityService clipboard capture on an emulator (API 29+ and
      34+), record the working mechanism and its limits in `.l00prite/memory.md` before
      building the full capture feature.
- [ ] `core/database`: Room timeline entity/DAOs, content-hash dedup, retention +
      auto-cleanup worker (pinned/favorite exempt).
- [ ] `core/design` theme (dynamic color + static fallback, light/dark) and `app/` shell:
      navigation, `ModuleRegistry` home grid, settings, onboarding with capture
      disclosure.
- [ ] `feature/clipboard`: timeline UI — list, search, pin, favorite, delete, category
      filters, item detail with type chips.
- [ ] Capture ladder integration: share-to-tactos target, manual add, foreground refresh,
      accessibility toggle wired to the spiked mechanism.
- [ ] v1 contextual actions: URL open/share/QR (zxing offline); color preview +
      HEX⇄RGB⇄HSL/HSV; JSON validate/beautify/minify; universal copy/share/pin.
- [ ] Settings: retention configuration, sensitive-clip policy; verify
      `EXTRA_IS_SENSITIVE` handling on an API 33+ image.
- [ ] Replace CI stubs with real workflows (assembleDebug + test + lint on PR); rewrite
      `README.md` (what tactos is, sideload install, capture disclosure, privacy
      posture).

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

## Done
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
