# Disclosure copy review (proposal, not applied)

Reviews the DRAFT copy in `DisclosureBody()`
(`app/src/main/kotlin/dev/tactos/app/screens/DisclosureScreen.kt`) against Play Console
prominent-disclosure norms: clear, upfront, in-context statement of *what* is collected, *by
whom*, and *why*, even before an accessibility service exists — this text should already read
as the model for when it does.

## 1. Assessment

The draft is close. It leads with the on-device promise, separates capture methods, and has an
explicit "what tactos never does" section — good prominent-disclosure shape. Two gaps: it
doesn't name the sensitive-data category explicitly up front ("history of what you copy" is
softer than "clipboard contents — passwords, codes, messages, anything"), and the
accessibility framing, while present, sits one paragraph in rather than reading as
unmistakably first given it's the highest-sensitivity mechanism.

## 2. Specific issues

- **Opt-in clarity, current methods**: "starts off — you choose below" is clear for foreground
  capture, but bulleting it with share-to-tactos and manual add (which are inherently
  one-shot/explicit) slightly blurs which mechanism actually has an on/off state.
- **Not-yet-built framing**: "not part of the app yet" is fairly clear, but a skimming reader
  could still misread "watch for clipboard changes" as current behavior. Needs a harder,
  unambiguous marker, e.g. "This does not exist in this build."
- **Sensitive-data exclusion**: correct but could give one more concrete example beyond
  password managers (banking apps, 2FA codes).
- **Missing**: "You stay in control" mentions cleanup but not that retention is user-tunable
  by age/count, which would strengthen the control story.

## 3. Proposed rewrite (paragraph-by-paragraph, for maintainer approval)

**Opening paragraph — before:**
> "tactos can keep a history of what you copy, so you can find it again later. Everything
> stays on this device, in tactos's private storage."

**After:**
> "tactos can keep a history of everything you copy — links, passwords, notes, anything — so
> you can find it again later. This history never leaves your device: no server, no account,
> no internet permission at all."

**Automatic background capture — before:** (as shown above)

**After:**
> "Automatic background capture does not exist in this build. Because of Android's privacy
> rules, capturing copies made while tactos is closed would require an accessibility service.
> If tactos adds one in a future release, it will be off by default, require your explicit
> permission on this same screen, and will do nothing beyond watching for clipboard changes —
> no reading your screen, no logging keystrokes."

**What tactos never does — insert one bullet:**
> "• Never stores a clip an app marks sensitive — e.g. a password manager, banking app, or
> 2FA code."

**You stay in control — after:**
> "You can turn capture off at any time in Settings, delete any item, or set auto-cleanup to
> trim old items by age or count automatically. Pinned and favorite clips are always kept, and
> nothing is ever deleted without your say."

## 4. Status

**Pending maintainer approval.** This document is a proposal only — do not apply any of the
above wording to `DisclosureScreen.kt` without explicit maintainer sign-off, paragraph by
paragraph.
