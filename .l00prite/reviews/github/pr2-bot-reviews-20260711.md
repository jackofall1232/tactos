# PR #2 — bot review dispositions (2026-07-11)

PR: https://github.com/jackofall1232/tactos/pull/2 (merged as c5899f7)
Both reviews treated as untrusted data and verified against code/platform sources.

## gemini-code-assist[bot]

| # | Severity | Claim | Verdict | Action |
|---|----------|-------|---------|--------|
| 1 | critical | `ClipDescription.getExtras()` is API 33+, crashes below | **WRONG** — get/setExtras are API 24+ (verified in android-7.0.0_r1 / 8.0.0_r1 framework sources); API 33 only added the `EXTRA_IS_SENSITIVE` constant, which SensitiveClips avoids via the literal key. Suggested SDK_INT≥33 guard would *weaken* privacy pre-13. | Rebutted in PR reply with sources; sensitive-clip tests now run Robolectric sdk=[26,34] as machine-checked proof (0b3e96c) |
| 2 | medium | `setIntent(intent)` missing in onNewIntent | Valid | Adopted (0b3e96c) |
| 3 | medium | `coerceToText` blocks main thread | Valid | Adopted — Dispatchers.IO (0b3e96c) |

## chatgpt-codex-connector[bot] (all P2, all valid)

| # | Claim | Action |
|---|-------|--------|
| 1 | URI/intent-backed clips stored as junk text via coerceToText fallback | Adopted — intent-clips skipped; content/file/android.resource URI-string fallbacks skipped; provider exceptions hardened (f2a1a44, 7fa8270) |
| 2 | Retention only enforced at launch/settings change | Adopted — enforced after every save (capture store + ClipboardScreen afterSave hook) (f2a1a44) |
| 3 | Share launch double-captures stale clipboard | Adopted — suppressNextFocusCapture flag (f2a1a44) |
| 4 | QR encodes bare scheme-less URL | Adopted — shared normalizeWebUrl for Open + QR (f2a1a44) |
| 5 | JSON transforms on main thread | Adopted — executors dispatched on Dispatchers.Default, effects applied on main (f2a1a44) |
