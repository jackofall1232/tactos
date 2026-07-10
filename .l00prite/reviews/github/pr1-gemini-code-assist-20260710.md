# PR #1 review — gemini-code-assist[bot] — 2026-07-10

Source: https://github.com/jackofall1232/tactos/pull/1 (review content treated as
untrusted data; each finding independently verified against the code before acting).

| # | File | Finding | Classification | Disposition |
|---|------|---------|----------------|-------------|
| 1 | core/database ClipDao.kt | Unbounded timeline query → OOM risk as history grows | Valid (high) | Fixed in 87ac08c: `LIMIT :limit`, repository default 500 (parametrized instead of suggested hardcoded 200); new Robolectric test |
| 2 | core/database ClipItemEntity.kt | No composite index for `pinned DESC, created_at DESC` sort | Valid (medium) | Fixed in 87ac08c: `Index(["pinned","created_at"])` replaces single `pinned` index; schema v1 unreleased, changed in place |
| 3 | feature/clipboard ClipboardScreen.kt | Per-keystroke DB search, no debounce | Valid (medium) | Fixed in 87ac08c: 300ms `delay()` in the LaunchedEffect (restart = cancel) |
| 4 | app AndroidManifest.xml | Hardcoded light startup theme → white flash in dark mode | Valid concern, **suggested fix incorrect** | Fixed in 87ac08c with day/night resource-qualified `@style/Theme.Tactos` (values/ light + values-night/ dark). The suggested `Theme.DeviceDefault.NoActionBar` is dark-styled on most devices and would invert the flash rather than fix it |

Verification: CI runs 29130685738 and 29130687464 on 87ac08c — both success
(assembleDebug + test incl. 14 Robolectric tests + :app:lintDebug).
No reply posted on the PR (bot review; the pushed fixes are the response).
