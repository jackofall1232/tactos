# ADR 0004 — Reusing code from ImageToolbox (Apache-2.0) in an MIT project

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

tactos's image toolbox (Phase 4, partially pulled forward) uses
[T8RIN/ImageToolbox](https://github.com/T8RIN/ImageToolbox) as a reference
implementation. ImageToolbox is Apache-2.0; tactos is MIT. Apache-2.0 code may
be included in an MIT project, but the copied portions remain Apache-2.0 and
must retain their license header and attribution (Apache-2.0 §4). ImageToolbox
itself must never be vendored wholesale: it carries ~120 dependencies including
Firebase (market flavor), unconditional INTERNET usage, and one CeCILL
(GPL-like) filter library — all incompatible with tactos's constraints.

## Decision

Three tiers, applied per file:

1. **Ports** — files whose substance (logic, semantics, data tables) comes from
   ImageToolbox. They keep the original Apache-2.0 header, add an
   `Adapted from ImageToolbox <upstream path>` line describing the changes, and
   are listed in this ADR and covered by the repo-level `NOTICE` file.
2. **Pattern-inspired rewrites** — files that borrow an *idea* (an interface
   shape, an algorithm outline) but are written fresh for tactos. These are
   plain MIT files; provenance is noted here, not in the file.
3. **Never imported** — anything from `core/ui`, `core/data` (wholesale),
   `feature/filters`/`core/filters` (G'MIC is CeCILL), Firebase/GMS/analytics
   code, network-dependent features, or any of the native codec AARs without a
   fresh dependency review.

### Current ports (tier 1)

| tactos file | Upstream source |
|---|---|
| `core/images/src/main/kotlin/dev/tactos/core/images/exif/ExifRemovalPreset.kt` | `feature/delete-exif/.../domain/model/ExifRemovalPreset.kt` |
| `core/images/src/main/kotlin/dev/tactos/core/images/exif/MetadataTag.kt` | simplified from `core/domain/.../image/model/MetadataTag.kt` |

### Current pattern-inspired rewrites (tier 2)

| tactos file | Upstream idea |
|---|---|
| `core/images/.../rename/` (pattern resolver, validator) | `feature/batch-rename` token-pattern renaming, re-tokenized for tactos (`{name}`, `{n}`, `{date}`, …) |
| `core/images/.../TargetSizeSearch.kt` | `feature/weight-resize` compress-to-target-size search |
| `feature/images/.../engine/ImageEncoders.kt` | `core/data/.../ImageCompressorBackend.kt` per-format encoder factory |

## Consequences

- The repo is "MIT plus enumerated Apache-2.0 files"; `NOTICE` satisfies the
  attribution requirement and this table is the source of truth.
- New ports require updating this ADR and `NOTICE` in the same commit.
- Dependency posture is unchanged: reusing code never implies importing the
  upstream dependency that backed it (e.g. tactos backs EXIF removal with
  androidx `ExifInterface`, not ImageToolbox's native exif AAR).
