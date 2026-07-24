---
name: bump-versions
description: >
  Checks whether this project is on the latest LTS release of Scala (project/Versions.scala's
  `scala` value) - the one version-tracking gap Renovate does not cover in this repo. Renovate
  already tracks and auto-PRs everything else (library deps, sbt plugins, sbt itself via
  project/build.properties, and even the standalone scalafmt formatter version in .scalafmt.conf)
  via its default managers - none of those need a manual check. See CLAUDE.md's "Versioning
  policy" for the full rationale. Use this whenever the user asks to bump/upgrade/update Scala,
  asks "are we on the latest Scala LTS", or asks a general "check for outdated deps" question (in
  which case, note that's Renovate's job and offer this narrower Scala-only check instead). If the
  user asks about sbt itself, scalafmt, a library dependency, a plugin, or a GitHub Action, tell
  them Renovate already automates that and point them at the open Renovate PRs / dependency
  dashboard instead.
---

# Bump versions

This project relies on **Renovate** (`renovate.json`) for dependency upkeep — including sbt itself
and the standalone scalafmt formatter version, via its default managers, not just what
`sbt: enabled` implies. See CLAUDE.md's "Versioning policy" for how that was confirmed.

The **only** thing Renovate can't track is the **Scala language version** itself
(`project/Versions.scala`'s `scala` value) — a bare string constant, not a resolvable
Maven-coordinate dependency. That's this skill's entire job.

If the user asks about anything else (a library, a plugin, sbt itself, scalafmt, a GitHub Action),
don't run a manual check — tell them Renovate already automates that and point them at the open
Renovate PRs or the Dependency Dashboard issue instead.

## Step 1 — Sync with the remote

```bash
git status   # must be clean
git pull
```

If `git status` shows local changes, **abort the skill** — don't pull or stash on the user's
behalf. Tell them to commit or stash first.

## Step 2 — Check the Scala version

```bash
curl -s "https://api.github.com/repos/scala/scala3/releases" | python3 -c "
import sys, json
data = json.load(sys.stdin)
lts = [r['tag_name'] for r in data if 'LTS' in (r.get('name') or '') and '-RC' not in r['tag_name']]
print('latest LTS:', lts[0] if lts else 'none found')
"
grep '^  val scala ' project/Versions.scala
```

## Step 3 — Apply the project's versioning policy

Latest LTS only, never a newer mainline release.

## Step 4 — Confirm scope with the user

If a newer LTS exists, don't migrate unprompted — a Scala version bump is the change here most
likely to cause real compile fallout (deprecations, `-Wunused` warnings, occasionally a genuine
inference difference like the one CLAUDE.md documents from the `3.3.8` migration). Ask before
touching it.

## Step 5 — Apply and verify (only if the user confirms a migration)

1. Edit `project/Versions.scala`'s `scala` value.
2. Verify:
   ```bash
   sbt scalafmtAll
   sbt compile
   sbt test
   ```
   Read any new compiler warnings/errors carefully — don't treat a clean `sbt compile` alone as
   sufficient signal given `-Wunused` and deprecation warnings are common across Scala versions.
   `sbt test` runs every module including `integration`'s live-network `*ITSpec` — a flaky failure
   there (e.g. a live Maven Central call) isn't necessarily the version bump's fault; rerun that
   one spec in isolation before concluding it's a real regression.
3. If it fails, stop — investigate before deciding whether to revert, fix the fallout, or flag it
   as needing more review than expected.

## Step 6 — Report

- **What changed** — old Scala version → new, and whether the verify pass was clean.
- **What came up** — anything unexpected, even on success: `git status` blocking the run, a newer
  LTS found but left unmigrated because the user didn't confirm. This is often more useful than
  the version diff itself.
