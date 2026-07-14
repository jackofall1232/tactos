# Manual device-verification checklist

This is a manual test script for the checks that **cannot** be verified in CI or in this
sandbox and require a real device or emulator. CI (`.github/workflows/ci.yml`) already
covers `assembleDebug` + `test` + `:app:lintDebug` on every push/PR — this document is
everything CLAUDE.md's [Definition of Done](../CLAUDE.md#4-definition-of-done) (Section 4)
still needs a human, on hardware, to sign off.

Run the entire checklist **twice**: once on an API 26 (Android 8.0, `minSdk`) device or
emulator, once on an API 34+ (a recent Android version) device or emulator. Each item below
has a result row with a checkbox for each API level — fill in both, plus notes for any
deviation or failure. This file is a template; copy the filled-in results into
`.l00prite/ledger.md` (or attach as a PR comment) as the verification evidence Section 4
requires — do not edit the `.l00prite/` directory from this checklist itself.

## Getting the build

**Verify the exact commit/branch being signed off, not just "the latest green `main`."**
Download the debug APK from the **`tactos-debug-apk`** artifact on the CI run for the
specific commit or PR head under review — open that commit's checks on
[the CI workflow](https://github.com/jackofall1232/tactos/actions/workflows/ci.yml), confirm
it's green, and grab the artifact from its "Artifacts" section. Testing an artifact from an
older green `main` run can produce clean-looking evidence for a build that doesn't contain
the changes actually being verified. Alternatively build locally from that exact commit with
`./gradlew assembleDebug` per the [README](../README.md#build-from-source) — APK lands at
`app/build/outputs/apk/debug/app-debug.apk`.

## Run metadata (fill in before starting)

| | API 26 run | API 34+ run |
|---|---|---|
| Device / emulator model | | |
| Android build / OS version | | |
| APK source (CI run URL or local commit hash) | | |
| Tester | | |
| Date | | |

---

## 1. Install and permission surface

**Maps to:** DoD #2 (device run), DoD #4 (no `INTERNET` permission)

Steps:
1. Sideload the debug APK (transfer + tap to install, or `adb install app-debug.apk`).
2. During install, note whether Android shows **any** runtime-permission grant screen.
3. After install, open **Settings → Apps → tactos → Permissions** and confirm the list is
   empty (no permissions requested, none granted).
4. Optional double-check: `adb shell dumpsys package dev.tactos.app | grep -i permission`
   should show no requested/granted app permissions of note.

Expected: no permission dialog at install; the Permissions screen shows nothing to grant;
`dumpsys` confirms it.

Result — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

---

## 2. First-run onboarding

**Maps to:** DoD #2, DoD #6 (README/onboarding describes what actually ships)

Steps:
1. Launch tactos for the first time (fresh install, no prior data).
2. Confirm the capture-disclosure text is shown before the home screen, and that it reads
   plainly (what's captured, that it's on-device, current mechanisms vs. not-yet-built
   ones — see `docs/disclosure-review.md` for the copy this should match once approved).
3. Confirm the capture-on-focus (foreground-refresh) toggle defaults to **off** on this
   screen.
4. Complete onboarding without turning the toggle on.
5. Force-stop the app (or just relaunch it) and reopen it.

Expected: onboarding does not reappear on relaunch; the app opens straight to the home
grid; the toggle state chosen during onboarding persists into Settings.

Result — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

---

## 3. Capture ladder

**Maps to:** DoD #2, partially — this section only exercises the rungs that exist in the
codebase today (share-to-tactos, foreground-refresh, manual add). CLAUDE.md Section 3 lists
accessibility-service capture as a required v1 ladder rung, and DoD #2 requires exercising
*each* ladder rung; passing 3a–3c alone is **not** sufficient to check off DoD #2 or declare
v1 complete. The accessibility rung is deliberately unbuilt this round (see
`docs/spikes/accessibility-capture-spike.md`) — do not expect or test one yet, and do not
treat a clean pass here as full ladder sign-off until it exists and is verified too.

### 3a. Share-to-tactos
Steps: from any other app, select some text and use the share sheet, choosing "tactos" as
the target.
Expected: the shared text appears at the top of the tactos timeline, correctly type-detected.
Result — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

### 3b. Foreground-refresh (capture-on-focus)
Steps: in tactos Settings, turn the capture-on-focus toggle **on**. Switch to another app,
copy some text there, then switch back to tactos (bring it to the foreground).
Expected: the copied text appears in the timeline without any manual action inside tactos.
Result — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

### 3c. Manual add
Steps: in tactos, use the "+" / manual-add affordance and type or paste a value directly.
Expected: the item appears in the timeline, correctly type-detected.
Result — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

---

## 4. Decline path (capture-on-focus left off)

**Maps to:** partially covers DoD #3, not a full sign-off. DoD #3 is specifically about
declining the **accessibility-service** capture prompt and confirming everything else still
works — that prompt and service do not exist in this build (see
`docs/spikes/accessibility-capture-spike.md`), so there is no accessibility-decline flow to
test yet. This section only exercises the narrower, already-shipped case of leaving
capture-on-focus off. **Leave the DoD #3 sign-off row itself pending** until the
accessibility service and its consent prompt exist and that specific decline path has been
tested — do not copy this section's result into the ledger as DoD #3 evidence.

Steps: on a **separate** fresh install (or after resetting app data), complete onboarding
leaving capture-on-focus off and never enable it. Exercise, without ever turning it on:
search, pin, favorite, delete, category filters/assignment, and every contextual action
(from sections 5–6 below) using items added via share-to-tactos and manual add only.

Expected: every one of those features works fully with capture-on-focus never enabled —
nothing is gated behind it.

Result (capture-on-focus-declined only, NOT DoD #3) — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
DoD #3 (accessibility-declined) — status: ☐ Pending (service not yet built)
Notes: ______________________________________________________________

---

## 5. Type detection and contextual actions

**Maps to:** DoD #2 (type detection and v1 contextual actions behave as specified)

Add one clip of each type below (via share or manual add) and confirm both the type badge
shown in the timeline/detail view and that the type-specific actions work.

| Type | Example to paste | Badge correct? (26 / 34+) | Type-specific action(s) work? (26 / 34+) |
|---|---|---|---|
| URL | `https://tactos.dev/example` | ☐ / ☐ | Open ☐/☐ · Share ☐/☐ · QR renders **and scans with another device/app** ☐/☐ |
| Email | `person@example.com` | ☐ / ☐ | (copy/share/pin as for any item) ☐/☐ |
| IPv4 | `192.168.1.1` | ☐ / ☐ | (copy/share/pin) ☐/☐ |
| IPv6 | `2001:db8::1` | ☐ / ☐ | (copy/share/pin) ☐/☐ |
| Hex color | `#3F8E7C` | ☐ / ☐ | Swatch preview correct ☐/☐ · HEX⇄RGB⇄HSL/HSV conversions correct ☐/☐ |
| RGB color | `rgb(63, 142, 124)` | ☐ / ☐ | Swatch preview correct ☐/☐ · conversions correct ☐/☐ |
| JSON | `{"a":1,"b":[true,null]}` | ☐ / ☐ | Validate ☐/☐ · Beautify produces indented output ☐/☐ · Minify produces compact output ☐/☐ |
| Phone number | `+1 415 555 0134` | ☐ / ☐ | (copy/share/pin) ☐/☐ |
| Plain text | `just some notes` | ☐ / ☐ | (copy/share/pin) ☐/☐ |

Also confirm, for every item regardless of type: copy, share, and pin all work
(cross-cutting v1 actions, per CLAUDE.md Section 3).

Notes: ______________________________________________________________

---

## 6. Sensitive-content handling (API 33+ only)

**Maps to:** DoD #2 (v1 contextual/behavioral spec includes sensitive-clip handling per
CLAUDE.md Section 2/3), and is a precondition of DoD #5 (Section 3 checkbox: "`EXTRA_IS_SENSITIVE`
handling as specified")

This item only applies to the API 34+ run (or any API 33+ device) — skip it on API 26.

Steps — either method:
- **Real app:** on API 33+, copy a password from a password manager or from a field marked
  as a password input (e.g. a saved Wi-Fi password: Settings → Network & internet →
  Internet → tap a saved network → Share → the displayed/copyable password field is a
  password-variation field, which Android 13+ auto-flags sensitive on copy). Then switch to
  tactos (with capture-on-focus on) and check the timeline.
- **Controlled fallback:** if no such app is available, a maintainer can build a minimal
  scratch app that constructs a `ClipData` with
  `description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }}`
  before calling `ClipboardManager.setPrimaryClip(...)`, then copy from that. This is a
  throwaway test aid only — do not add it to the tactos repo.

Expected: the sensitive clip is **never** stored in the timeline, not even masked (per
CLAUDE.md Section 2: "skip, or store masked with explicit user reveal" — confirm which
behavior is actually implemented and that it matches what's documented).

Result — API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

---

## 7. Retention / auto-cleanup

**Maps to:** DoD #5 (Section 3 checkbox: auto-cleanup sparing pinned/favorite items)

Note: `0` for either setting means "keep forever" (unlimited) — it does not mean "delete
everything." Set a **positive** value to actually exercise cleanup.

Steps — add and protect clips *before* lowering the retention limit, so cleanup can't run
out from under you mid-setup:
1. With retention still at its defaults (`0`/`0`, unlimited), add several clips, then pin or
   favorite at least one of them — this is the clip that should survive the trim.
2. *Now* go to Settings and set a low max-items value (e.g. 3, lower than your current clip
   count) and/or a short-but-positive retention-days value (e.g. 1 day, using a clip you can
   backdate, or a value you're prepared to wait out — `0` days does nothing).
3. Relaunch the app (cleanup runs on next launch per CLAUDE.md Section 2).

Expected: the timeline is trimmed down to the configured limit/window; pinned and favorite
items survive the trim regardless of age/count.

Result — API 26: ☐ Pass ☐ Fail   API 34+: ☐ Pass ☐ Fail
Notes: ______________________________________________________________

---

## 8. Dark/light mode and dynamic color

**Maps to:** DoD #2 (Section 3 checkbox: Material 3 theme with dynamic color + static
fallback, light and dark)

Steps:
1. Toggle system dark mode off/on and confirm the app follows it in both directions
   (colors, text contrast, no unstyled/default-theme flashes).
2. On a device/OS that supports Material You dynamic color (Android 12+, so relevant to the
   API 34+ run): change the system wallpaper/accent color and confirm tactos's theme
   updates to match.
3. On the API 26 run (no dynamic color available) or any device without it: confirm the
   static tactos teal/amber fallback palette renders correctly in both light and dark mode.

Result — API 26 (static palette, light/dark): ☐ Pass ☐ Fail
Result — API 34+ (dynamic color, light/dark): ☐ Pass ☐ Fail
Notes: ______________________________________________________________

---

## Definition-of-Done cross-reference

Quick summary of which Section 4 bullets this checklist retires evidence for, and which it
deliberately leaves to other processes:

| Section 4 bullet | Covered by |
|---|---|
| 1. `assembleDebug` / `test` / `lint` pass in CI | Not this doc — CI (`.github/workflows/ci.yml`), already automated |
| 2. APK sideloads and runs on API 26 and API 34+: capture ladder, search/pin/favorite/cleanup, type detection, contextual actions | Sections 1–3, 5, 7, 8 of this checklist |
| 3. Declining accessibility/automatic capture leaves everything else working | Section 4 of this checklist |
| 4. No `INTERNET` permission in the v1 APK | Section 1 of this checklist (device-side spot check); authoritative check is the merged-manifest review already gating any manifest change |
| 5. Every Section 3 v1 checkbox checked with a pointer to verification evidence | This checklist, once filled in and copied into `.l00prite/ledger.md`, is that evidence for the device-only items; the rest of Section 3 is verified by CI |
| 6. README documents what actually exists | Not this doc — a docs/README review item |
| 7. Maintainer has reviewed and merged v1 to `main` | Not this doc — a maintainer sign-off step, happens after all of the above |
