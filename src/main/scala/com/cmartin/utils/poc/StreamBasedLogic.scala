package com.cmartin.utils.poc

import com.cmartin.utils.poc.StreamBasedLogic.Dependency._
import zio.stream.ZStream
import zio.{Task, UIO, ZIO}

import scala.util.matching.Regex

object StreamBasedLogic {
  sealed trait Dependency

  object Dependency {
    case class MavenDependency(g: String, a: String, v: String) extends Dependency

    object MavenDependency {
      def fromRegexMatch(regexMatch: Regex.Match): MavenDependency =
        MavenDependency(
          regexMatch.group(1), // group
          regexMatch.group(2), // artifact
          regexMatch.group(3)  // version
        )

    }

    case class InvalidDependency(line: String, parseError: String) extends Dependency

    case class MissingRemoteDependency(dep: MavenDependency, error: String) extends Dependency

    case class RemoteDependency(local: MavenDependency, remote: MavenDependency) extends Dependency
  }

  private val pattern: Regex =
    "(^[a-z][a-z0-9-_.]+):([a-z0-9-_.]+):([0-9]{1,2}\\.[0-9]{1,2}[0-9A-Za-z-.]*)".r

  def getLinesFromFilename(filename: String): ZStream[Any, Throwable, String] =
    ZStream.fromIteratorScoped(
      ZIO.fromAutoCloseable(
        ZIO.attempt(scala.io.Source.fromFile(filename))
      ).map(_.getLines())
    )

  def parseDepLine(line: String): Task[Dependency] = {
    def extractDependency(iterator: Iterator[Regex.Match]) =
      if (iterator.hasNext) ZIO.succeed(MavenDependency.fromRegexMatch(iterator.next()))
      else ZIO.succeed(InvalidDependency(line, "regex does not match"))

    for {
      _             <- ZIO.log(s"parsing line: $line")
      matchIterator <- ZIO.succeed(pattern.findAllMatchIn(line))
      result        <- extractDependency(matchIterator)
    } yield result
  }

  def isValidDep(dep: Dependency): Boolean =
    dep match {
      case MavenDependency(_, _, _) => true
      case _                        => false
    }

  private def processMavenDep(dep: MavenDependency): UIO[MavenDependency] =
    ZIO.log(s"valid dependency: $dep") *> ZIO.succeed(dep)

  private def processInvalidDep(dep: InvalidDependency): UIO[InvalidDependency] =
    ZIO.log(s"invalid dependency: $dep") *> ZIO.succeed(dep)

  def processStreams(
      validStream: ZStream[Any, Throwable, Dependency],
      invalidStream: ZStream[Any, Throwable, Dependency]
  ): ZStream[Any, Throwable, Dependency] =
    ZStream.mergeAll(2)(
      validStream.collectType[MavenDependency]
        .mapZIO(processMavenDep),
      invalidStream.collectType[InvalidDependency]
        .mapZIO(processInvalidDep)
    )

}