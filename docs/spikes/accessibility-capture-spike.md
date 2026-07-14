# Spike: does an AccessibilityService see clipboard changes while backgrounded?

**Status:** not yet run. This is a protocol, not a result. No accessibility service, manifest
entry, or settings/disclosure wiring exists yet, and none should until this spike produces
real data — see "Gate" at the bottom.

## Why

Since Android 10, background apps cannot read the system clipboard. `AccessibilityService` has
historically been a sideload-viable workaround, but exactly which accessibility events fire,
how reliably, and with what latency varies by API level and OEM (battery-optimization
throttling especially). CLAUDE.md is explicit: don't build the capture-ladder rung on an
assumption — measure it on real devices/emulators first and record the result in
`.l00prite/memory.md`.

## API levels to test

| API | Why it matters | Emulator image |
|---|---|---|
| 26 | `minSdk` floor | Android 8.0 (API 26), Google APIs, x86_64 |
| 29 | Background clipboard read restriction introduced | Android 10 (API 29), Google APIs |
| 33 | `ClipDescription.EXTRA_IS_SENSITIVE` formalized | Android 13 (API 33), Google APIs |
| 34 | Current `targetSdk`-adjacent | Android 14 (API 34), Google APIs |

Also try one physical OEM device per major skin (Samsung/One UI, Xiaomi/MIUI or a Pixel) if
available — background-service throttling is often OEM-specific and doesn't reproduce on the
emulator.

## Illustrative pattern to try

Copy this into a throwaway experimental branch or a standalone scratch module — **never**
straight into `app/` or any shipped Gradle module — until results are in:

```kotlin
// EXPERIMENTAL ONLY — not part of any shipped module.
class ClipboardSpikeAccessibilityService : AccessibilityService() {

    private lateinit var clipboardManager: ClipboardManager

    override fun onServiceConnected() {
        clipboardManager = getSystemService(ClipboardManager::class.java)
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val clip = clipboardManager.primaryClip ?: return
        val desc = clip.description
        val sensitive = desc.extras
            ?.getBoolean("android.content.extra.IS_SENSITIVE", false) ?: false
        val text = clip.getItemAt(0)?.text?.toString().orEmpty()
        // Never log the actual clipboard contents -- this spike is explicitly meant to be
        // run against password-manager/2FA sources, and raw text in Logcat can leak real
        // secrets to device logs. Log only redacted, non-reversible metadata.
        Log.d("ClipSpike", "event=${event.eventType} sensitive=$sensitive length=${text.length}")
    }

    override fun onInterrupt() {}
}
```

## What to observe and log, per API level

- Does the callback fire at all when the clipboard changes while tactos is **fully
  backgrounded** (not merely paused/covered by another activity)?
- Any interference from OEM battery optimization / app-standby buckets? Does disabling
  battery optimization for the spike app change the result?
- Latency from copy event to callback firing (rough wall-clock is fine).
- Copy from a password-manager-like source (or any app that sets
  `ClipDescription.EXTRA_IS_SENSITIVE`): does the sensitive flag survive to the accessibility
  callback's `ClipDescription`? If yes, the service can skip storage the same way
  `core/clipboard/SensitiveClips.kt` already does for the other capture-ladder rungs.

## Results template

Copy this table into `.l00prite/memory.md` once real runs are done — one row per
device/API combination:

```yaml
- api_level: 26
  fired_while_backgrounded: null   # yes | no
  latency_ms: null
  sensitive_flag_respected: null   # yes | no
  device: null                    # e.g. "Pixel 6 emulator" or "Samsung A54, One UI 6"
  notes: null
```

| API level | Fired while backgrounded | Latency | Sensitive-clip flag respected | OEM/device | Notes |
|---|---|---|---|---|---|
| 26 | | | | | |
| 29 | | | | | |
| 33 | | | | | |
| 34 | | | | | |

## Gate

Results from this spike gate whether and how the real accessibility service, its
`AndroidManifest.xml` service declaration, and its settings/disclosure wiring get built — none
of that is in scope until this spike has real results recorded in `.l00prite/memory.md`.
