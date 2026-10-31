# CLAUDE.md

## What this is

A Scala/ZIO CLI that scans a project's Maven dependency list, queries Maven Central for the latest version of each `(groupId, artifactId)`, and reports newer versions (Semver-aware via `just-semver`).

## Commands

Versions are pinned in `project/build.properties` and `project/Versions.scala`.

```
sbt compile
sbt application/test                 # fast unit tests, application module only
sbt xtest                            # all fast tests, excludes integration; use this for CI-like runs
sbt test                             # everything, including integration's live-network *ITSpec
sbt "testOnly *VersionManagerSpec"   # single spec
sbt integration/test                 # *ITSpec only; hits real network and filesystem
sbt xcoverage                        # clean;coverage;xtest;coverageReport, excludes integration
sbt scalafmtAll                      # format; sbt scalafmtCheckAll is the CI check
sbt assembly                         # fat jars for every module
sbt xdup                             # dependencyUpdates
sbt xdeplist                         # dependencyList to /tmp/dep-analyzer.log
sbt application/stryker              # mutation testing, see below
```

`xstart`/`xstop` are broken: `sbt-revolver` isn't in `project/plugins.sbt`.

The packaged app needs env vars `DL_FILENAME` (dependency-list log) and `DL_EXCLUSIONS` (comma-separated groupId prefixes to skip). See README.md for the full run sequence.

## Modules

- **`application`**: the app (`com.cmartin.utils`). `src/main/scala/com/cmartin/utils/poc/` holds unrelated ZIO/Kafka experiments that aren't wired into `DependencyAnalyzerApp`.
- **`integration`**: `*ITSpec` tests against real files and network. Runs weekly or manually through `.github/workflows/integration-tests.yml`, not on push.
- **`scraper`**, **`zio-http`**: unrelated learning modules.

Local Kafka for the PoCs: `docker-kafka/docker-compose.yml` (see `docs/poc-stream-based.md`).

## Architecture (`application`)

**ZIO 2 + ZLayer** with the service pattern: a `trait` in `domain/` with a companion exposing `ZIO.serviceWithZIO` accessors, and an implementation providing a `ZLayer`.

| Service trait | Purpose | Implementation |
|---|---|---|
| `IOManager` | read lines from file, log results | `file.FileManager` |
| `LogicManager` | parse/filter/exclude GAV lines, compute valid rate | `logic.DependencyLogicManager` |
| `HttpManager` | query Maven Central | `http.ZioHttpManager` (uses `http.HttpClientManager`, sttp4) |

`config/ConfigHelper.applicationLayer` composes all layers. `ConfigHelper.readFromEnv` reads the env vars. Logging goes ZIO Logging → SLF4J → Logback.

**Pipeline (`DependencyAnalyzerApp.run`):**

1. `IOManager.getLinesFromFile`: read the raw lines.
2. `LogicManager.parseLines`: regex-parse into `Gav` or a failure (`ParsedLines`).
3. `LogicManager.excludeFromList`: drop excluded group prefixes.
4. `HttpManager.checkDependencies`: query `search.maven.org/solrsearch/select` and build `GavPair`s, collecting `DomainError`s.
5. `IOManager.filterUpgraded`: keep pairs where `GavPair.hasNewVersion`.

`domain/Model.scala` has the domain types and Maven response shapes (decoded with `zio-json`). Of `Gav`'s two orderings, `ord` (Semver-aware) is the one used. The `DomainError` channel isn't fully threaded to the exit code yet (see the `TODO`s in `DependencyAnalyzerApp`).

## Conventions

- Run `sbt scalafmtAll` before committing.
- A new capability follows the service pattern and is wired into `ConfigHelper.applicationLayer`.
- Network or filesystem tests are `*ITSpec` in `integration`; pure-logic tests are `*Spec` in `application`.

## Mutation testing (Stryker4s)

`stryker4s.conf` mutates `application/src/main/scala/**`, excluding `poc/**`. `.github/workflows/mutation-testing.yml` runs it on Fridays at 15:00 UTC or manually. It's report-only (`thresholds.break = 0`), posts the score to the step summary and uploads the HTML report. It isn't part of `ci.yml`.

The score is low, and that's genuine: `application`'s unit tests barely touch production code, and the tests exercising `HttpManager`/`LogicManager` are `*ITSpec`s, which Stryker doesn't run.

These caused real failures, so don't re-diagnose them:

- **`InitialTestRunFailedException` means the env vars are missing.** Stryker forks its test JVMs outside sbt's `Test / envVars`, so `FileManagerSpec` needs `DL_FILENAME`/`DL_EXCLUSIONS` from the shell. The workflow sets them; for a local run, export the same values as `build.sbt`.
- **`legacy-test-runner = true` is required.** The default runner tags every mutant `[NoCoverage]`. The legacy runner also needs more heap, hence `.jvmopts` (`-Xmx4096m`).
- **Concurrency is set in `stryker4s.conf`.** The plugin has no CLI options: `sbt application/stryker --concurrency 1` fails with `Not a valid command: --`.
- **The step needs `set -o pipefail`.** Without it, `| tee` hides a failing run behind a green check. If a run is green, check that the log has a `Mutation score` line.
- **To debug**, set `debug { log-test-runner-stdout: true }` in `stryker4s.conf` (sbt's `logLevel` doesn't reach the forked runner). `strykerTestFilter` isolates one spec.

## Versioning policy

- **Scala**: always the latest LTS or newer (`project/Versions.scala`), never a mainline release. Renovate can't track this bare string, so check it with the `bump-versions` skill.
- **Everything else** (libraries, sbt plugins, sbt, scalafmt): Renovate tracks and automerges these (`renovate.json`), so no manual check is needed. The exception is `confluentinc/cp-kafka`, which has automerge off because no CI job starts it.

A tuple pattern as a `for`-generator's LHS (`(a, b, c, d) <- x <&> y`) needs `withFilter`, which `ZIO` doesn't provide under stricter Scala 3 inference. Bind one name and use `._1` instead.
