package com.cmartin.utils

import com.cmartin.utils.config.ConfigHelper
import com.cmartin.utils.config.ConfigHelper._
import com.cmartin.utils.domain.Model.DomainError
import com.cmartin.utils.domain.{HttpManager, IOManager, LogicManager}
import zio._

/** Helper application for keeping a project's dependencies up to date. Using
  * ZIO effect ZIO[R, E, A]
  *   - R: application services / dependencies
  *   - E: error in case of failure
  *   - A: returned value
  *
  * Examples:
  *   - R = { ConfigManger, FileManager, LogicManager, HttpManager }
  *   - E: ConnectionError (ADT member)
  *   - A: Either[Exception, Results]
  *
  * `R` parameter is similar to dependency injection and the `provide` function
  * can be thought of as `inject`.
  */
object DependencyAnalyzerApp
    extends ZIOAppDefault {

  override val bootstrap: ULayer[Unit] = ConfigHelper.loggingLayer

  override def run
  // : IO[DomainError, Unit]
  = {

    // TODO resolve error channel type, actual Object
    val logicProgram =
      for {
        _           <- printBanner("Dependency Analyzer")
        config      <- readFromEnv()
        startTime   <- getMillis()
        lines       <- IOManager.getLinesFromFile(config.filename)
        parsedLines <- LogicManager.parseLines(lines) @@ iterablePairLog("parsingErrors")
        _           <- LogicManager.calculateValidRate(lines.size, parsedLines.successList.size) @@
                         genericLog("valid rate of dependencies")
        finalDeps   <- LogicManager.excludeFromList(parsedLines.successList, config.exclusions)
        results     <- HttpManager.checkDependencies(finalDeps)
        // TODO process errors
        _           <- IOManager.filterUpgraded(results.gavList) @@ iterableLog("upgraded dependencies")
        _           <- IOManager.logWrongDependencies(results.errors)
        _           <- calcElapsedMillis(startTime) @@ genericLog("processing time")
      } yield 0

    // main program
    logicProgram
      .provide(applicationLayer)

    //.mapError(_ => 1)
      //.fold(toExitCode,toExitCode)
      //.flatMap(code => exit(code))
//      .catchAllCause(cause =>
//        ZIO.logError(s"${cause.prettyPrint}")
//          .exitCode
//      )

    // .tapError(e => ZIO.logError(s"application error: $e"))
    // .mapError(_ => 1)
    // .fold(toExitCode, toExitCode)
    // .flatMap(exit)
  }

}
