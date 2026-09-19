# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Scala/ZIO CLI tool that scans a project's Maven dependency list, queries Maven Central for the latest version of each `(groupId, artifactId)`, and reports which dependencies have newer versions available (with Semver awareness via `just-semver`).

## Commands

Build tool: **sbt** (see `project/build.properties` for the pinned version). Scala 3 (`project/Versions.scala`).

```
sbt compile                          # compile all modules
sbt application/test                 # fast unit tests only (application module)
sbt xtest                            # alias: application/test;scraper/test;zio-http/test — the fast/unit test phase, excludes integration
sbt test                             # ALL modules' tests, including integration's live-network *ITSpec — no root-level exclusion; prefer xtest for CI-like runs
sbt "testOnly *VersionManagerSpec"   # run a single spec
sbt xcoverage                        # clean;coverage;xtest;coverageReport (alias, see build.sbt) — also excludes integration
sbt scalafmtAll                      # format all modules per .scalafmt.conf
sbt scalafmtCheckAll                 # format check (CI-enforced)
sbt assembly                         # build fat jars for every module (custom merge strategy in build.sbt)
sbt xdup                             # dependencyUpdates alias — check for outdated deps
sbt xdeplist                         # dependencyList/toFile /tmp/dep-analyzer.log -f
sbt integration/test                 # integration module only — *ITSpec, hits real network/filesystem; runs in CI as a separate non-blocking step (continue-on-error)
sbt application/stryker              # mutation testing (Stryker4s); config in stryker4s.conf — currently broken, see "Mutation testing" below
```

Other command aliases defined in `build.sbt`: `xreload`, `xstart`/`xstop` (revolver), `xupdate`.

Running the packaged app requires env vars `DL_FILENAME` (path to a dependency-list log file) and `DL_EXCLUSIONS` (comma-separated groupId prefixes to skip) — see README.md for the full standalone/Docker run sequence.

## Module layout

sbt multi-module build (`build.sbt`):

- **`application`** — the actual dependency-analyzer app (`com.cmartin.utils`). Also contains a grab-bag of unrelated ZIO/Kafka proof-of-concept code under `application/src/main/scala/com/cmartin/utils/poc/` (Kafka producer/consumer demos, banking domain PoC, ZIO layer/loop demos) — these are experiments, not part of the dependency-analyzer flow; don't assume they're wired into `DependencyAnalyzerApp`.
- **`integration`** — depends on `application`; holds `*ITSpec` tests that exercise real files/network (`FileManagerITSpec`, `HttpManagerITSpec`, `LogicManagerITSpec`, `ZioHttpManagerITSpec`, `ZStreamPocITSpec`, `SttpITSpec`), separate from the fast unit tests in `application`.
- **`scraper`** — unrelated standalone HTML-scraping utility (`dev.cmartin.scrapper`) using scala-scraper/Jsoup.
- **`zio-http`** — unrelated standalone ZIO HTTP server demo (`dev.cmartin.learn`) with a currency lookup endpoint.

Only `application` (+ `integration` for its tests) matters for the dependency-analyzer feature itself; `scraper` and `zio-http` are separate learning/demo modules that happen to live in the same repo.

## Architecture (application module)

Everything lives under `com.cmartin.utils`, wired together with **ZIO 2 + ZLayer** dependency injection using the standard "service pattern": each capability is a `trait` with a companion `object` exposing `ZIO.serviceWithZIO` accessors, and a concrete implementation elsewhere providing a `ZLayer`.

| Service trait (`domain/`) | Purpose | Implementation | Layer |
|---|---|---|---|
| `IOManager` | read dependency lines from file, log results | `file.FileManager` | `FileManager.layer` |
| `LogicManager` | parse/filter/exclude GAV lines, compute valid rate | `logic.DependencyLogicManager` | `DependencyLogicManager.layer` |
| `HttpManager` | query Maven Central for latest versions | `http.ZioHttpManager` (uses `http.HttpClientManager`, sttp4) | `ZioHttpManager.layer` |

All layers are composed in `config/ConfigHelper.applicationLayer` (`ZLayer.make[ApplicationDependencies]`), which also wires the sttp `WebSocketStreamBackend` and a `Clock`. Logging goes through ZIO Logging → SLF4J → Logback (`ConfigHelper.loggingLayer`, configured in `application-config.hocon` / `logback.xml`).

App config (`DL_FILENAME`, `DL_EXCLUSIONS`) is read from environment variables via `zio.Config` + `ConfigProvider.envProvider` in `ConfigHelper.readFromEnv`.

### Pipeline (`DependencyAnalyzerApp.run`)

1. `IOManager.getLinesFromFile` — read raw lines from the dependency-list log.
2. `LogicManager.parseLines` — regex-parse each line into a `Gav(group, artifact, version)` or a failure, returning `ParsedLines(failedList, successList)`.
3. `LogicManager.excludeFromList` — drop GAVs whose group matches a configured exclusion prefix.
4. `HttpManager.checkDependencies` — for each remaining GAV, query Maven Central (`search.maven.org/solrsearch/select`) and pair local vs. remote (`GavPair`), collecting `DomainError`s for failures.
5. `IOManager.filterUpgraded` — keep only pairs where `GavPair.hasNewVersion`, format for output.

Domain types (`domain/Model.scala`): `Gav`, `GavPair` (`hasNewVersion` compares versions), `DomainError` ADT (`FileIOError`, `ConfigError`, `NetworkError`, `WebClientError`, `ResponseError`, `DecodeError`, `UnknownError`), and the Maven Solr JSON response shapes (`MavenSearchResult` → `MavenResponse` → `Artifact`), decoded with `zio-json`. `Gav` has two `Ordering` instances — `ord` (Semver-aware via `just.semver.SemVer`, descending) is the one actually used; `ordOld` is a plain string-compare kept for reference.

Error channel is `DomainError`, not yet fully threaded through to the app's exit code (see `TODO` comments in `DependencyAnalyzerApp` around error handling / exit codes).

## Conventions

- Formatting is enforced by scalafmt (`.scalafmt.conf`, Scala 3 dialect, 120 col, `align.preset = most`) — run `sbt scalafmtAll` before committing.
- New capabilities should follow the existing service pattern: trait in `domain/`, accessor object with `ZIO.serviceWithZIO`, implementation + `ZLayer` in its own subpackage, wired into `ConfigHelper.applicationLayer`.
- Integration tests (network/filesystem-touching) belong in the `integration` module as `*ITSpec`; fast/pure-logic tests stay as `*Spec` in `application`.

## Mutation testing (Stryker4s)

`stryker4s.conf` (repo root, HOCON) configures Stryker4s, run via `sbt application/stryker`. Mutates `application/src/main/scala/**/*.scala`, excluding `poc/**` (see "Module layout") since that's unrelated PoC/demo code with no test coverage by design and would otherwise drag the score down.

**Status: root-caused and fixed on 2026-09-18** (was broken/no-baseline from 2026-07-24 through 2026-09-18 — history kept below for context, but don't re-diagnose the `InitialTestRunFailedException` from scratch):

- **Root cause**: `application/src/test/.../FileManagerSpec.scala`'s `"should read system property"` test calls `ConfigHelper.readFromEnv()` and asserts the result against a hardcoded `AppConfig("/tmp/dep-list.log", ...)`, which requires the `DL_FILENAME`/`DL_EXCLUSIONS` env vars to be set. `build.sbt` sets these via `Test / envVars` for sbt's own `Test/test` task fork — but `sbt-stryker4s`'s test-runner JVMs are forked through its own mechanism, entirely bypassing sbt's `Test/fork`/`Test/envVars` machinery. Those forked JVMs inherit only the *ambient shell/process* environment, so without `DL_FILENAME`/`DL_EXCLUSIONS` set at the OS level before invoking sbt, that one test genuinely fails inside Stryker's fork — which is exactly what `InitialTestRunFailedException` was correctly (if unhelpfully) reporting all along. Confirmed by enabling Stryker4s's own `debug { log-test-runner-stdout: true }` (config-file-only setting, **not** the same as sbt's `ThisBuild / logLevel := Level.Debug` used in the 2026-07-24 attempt below — that only affects sbt's own logging, not the forked test-runner's stdout) and reading the raw `Tests: succeeded 19, failed 1` count Stryker had been swallowing.
- **Fix**: `.github/workflows/mutation-testing.yml`'s `Mutation testing (Stryker4s)` step now sets `DL_FILENAME`/`DL_EXCLUSIONS` under `env:`, matching `build.sbt`'s `Test / envVars` values. For a local run, export the same two vars in the shell before `sbt application/stryker`.
- **Bisection method, for future reference**: `strykerTestFilter := Seq("com.cmartin.zio.FileManagerSpec")` (sbt setting, not `stryker4s.conf`) isolates a single spec without editing the tracked mutate glob; `strykerMutate := Seq("src/main/scala/com/cmartin/utils/nonexistent/*.scala")` (a glob matching zero files) proved the failure was unrelated to *which* file was mutated, or to mutation at all — the initial test run still failed with an empty mutate set, pointing at the test-runner's environment rather than Stryker's instrumentation.
- **NoCoverage follow-on, resolved 2026-09-19**: the default (non-legacy) runner tagged all 72 mutants `[NoCoverage]` (0.0%) even though the initial run passed; disabling `Test / fork` did not help. `legacy-test-runner = true` (now set in `stryker4s.conf`) gives real results: **2.78% mutation score** (79 s locally at `concurrency = 1`). The low score is genuine, not a tooling artifact: `application`'s unit tests (`VersionManagerSpec`, `FileManagerSpec`) barely touch production code; the tests that exercise `HttpManager`/`LogicManager` are `*ITSpec`s in the `integration` module, which Stryker doesn't run. Also note: `poc/Zio2Layers.scala` still received a mutant despite the `!...poc/**` glob (not yet investigated).
- **CI flag bug, fixed 2026-09-19**: the workflow ran `sbt application/stryker --concurrency 1`, which sbt parsed as its own command (`Not a valid command: --`), failing every run since at least 2026-08-28. Concurrency now lives in `stryker4s.conf`.
- **2026-07-24 original investigation** (kept for history): fixed a real bug where `ZStreamPocSpec` walked the whole repo tree asserting the discovered module set was exactly `{application, integration, scraper, zio-http}` — Stryker's `application/target/stryker4s-<uuid>/src/main/scala/...` sandbox was picked up as a spurious 5th module; fixed by excluding `target/` from the scan. Necessary, but not sufficient on its own — the env-var issue above was the remaining blocker. Also ruled out (correctly, but not the actual cause): sbt 2.0.3 vs 1.12.14, JDK 25 vs 21, execution-sandbox socket/IPC restrictions, and the local dev machine itself (reproduced identically on GitHub's own Ubuntu 24.04 runner).
- **2026-07-30 / 2026-09-18 updates** (kept for history): a `sbt-stryker4s` `1.0.x` → `1.1.0` bump temporarily changed the failure to a `SourceSplice.render` `StringIndexOutOfBoundsException` (a real regression in that release); `1.1.1` fixed that regression and reverted to the original `InitialTestRunFailedException` shape, which is the one actually root-caused above.

The project's sbt/JDK pins were lowered regardless of the Stryker outcome — both were very new; `sbt 1.12.14` / JDK 21 are the proven baseline the sibling `scala-3` project already runs cleanly on:
- `project/build.properties`: `sbt.version=1.12.14`
- `.github/actions/setup-jdk/action.yml`: `java-version: '21'` (shared by `ci.yml` and this workflow)

`.jvmopts` (`-Xmx4096m`, repo root) is kept because `legacy-test-runner = true` is now in use — it needs more heap than sbt's 1GB default once forking subprocesses.

Not part of the main push-triggered CI (`ci.yml`) — mutation runs are slow and exploratory. `.github/workflows/mutation-testing.yml` runs it weekly (Friday 15:00 UTC) plus on manual `workflow_dispatch`, posting the score to the run's step summary and uploading the HTML report as a build artifact (90-day retention). It's report-only regardless: `thresholds.break = 0` in `stryker4s.conf` until a baseline mutation score exists on this codebase (see the `NoCoverage` follow-on finding above — there isn't one yet, even with the exception fixed).

The workflow's first-ever run (2026-07-24, manual `workflow_dispatch`) showed **green despite Stryker genuinely failing** — the step piped `sbt application/stryker | tee stryker-output.log`, and GitHub's default `bash -e {0}` doesn't enable `pipefail`, so the step's exit code came from `tee` (always 0) instead of `sbt`. Fixed with an explicit `set -o pipefail`. If a run ever shows green again, verify the step log actually contains a `Mutation score` line before trusting it — don't rely on the checkmark alone for this workflow.

## Versioning policy

- **Scala** — pinned to `3.3.8` (`project/Versions.scala`), the current LTS; policy is **LTS-only**, mirroring the sibling `scala-3` project. This is Renovate's one blind spot (a bare string constant, not a resolvable dependency) — check manually via the `bump-versions` skill.
  - Gotcha from the 2026-07-24 migration off `3.6.4`, already fixed: a 4-tuple pattern used directly as a `for`-generator's LHS (`(cs, as, ls, fs) <- ... <&> ...`) in `application/.../poc/banking/BankingGlobalPositionPoc.scala` needed `withFilter`, which `ZIO` doesn't provide, under 3.3.8's stricter inference of `Zippable.Out`. Fixed by binding the zipped result to one name and accessing `._1`..`._4` in the `yield` instead — avoids the tuple-pattern generator, no new dependency. A future Scala bump shouldn't hit this again unless a similar pattern is reintroduced.
- **Everything else** — direct/transitive library deps, sbt plugins, **sbt itself** (`project/build.properties`), and the **standalone scalafmt formatter version** (`.scalafmt.conf`) are all tracked and auto-PR'd by Renovate already (`renovate.json`: `automerge: true`, `sbt: enabled` — confirmed via this repo's "Dependency Dashboard" issue; e.g. PR #154 bumped `.scalafmt.conf`, #156 bumped `project/build.properties`). This tracking comes from Renovate's default managers, not the `sbt` config block — no extra `renovate.json` config needed. No manual check needed for any of this.
