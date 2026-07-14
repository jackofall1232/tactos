# 0003 — Ratify the applicationId

## Status

Accepted. The maintainer explicitly ratified this on 2026-07-14, in-session.

## Context

tactos is pre-release: v1 (Clipboard OS core + shell) is still being built and no signed
release has shipped yet. Distribution is sideload-first, with the Play Store considered
later. `app/build.gradle.kts` already sets `applicationId = "dev.tactos.app"`, following a
reverse-domain pattern for the tactos brand (`dev.tactos.*`). Because this value has been in
continuous use since the initial scaffold, it needs to be formally locked in before the
first tagged release rather than left as an implicit default.

## Decision

`dev.tactos.app` is ratified as the final, immutable Android `applicationId` for tactos. It
will not change once the app is first published (sideload release or Play Store listing).

## Consequences

- The id is locked in ahead of the v0.1.0 tag; no further bikeshedding before first release.
- Android's `applicationId` cannot change after a signed release without becoming a new
  package/listing. Any future rename would require a new `applicationId` and thus a new Play
  Store listing that existing installs would need to migrate to.
- Any proposal to change it after first publication requires a new ADR and a human review
  gate (release/Play-Store-facing change).
