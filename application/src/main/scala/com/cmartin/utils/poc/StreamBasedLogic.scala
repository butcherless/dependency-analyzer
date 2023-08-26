package com.cmartin.utils.poc

import com.cmartin.utils.poc.StreamBasedLogic.Dependency._
import zio.json._
import zio.kafka.serde.Serde
import zio.stream.ZStream
import zio.{UIO, ZIO}

import scala.util.matching.Regex

object StreamBasedLogic {

  sealed trait Dependency extends Product with Serializable

  object Dependency {
    case class MavenDependency(g: String, a: String, v: String) extends Dependency

    object MavenDependency {
      def fromRegexMatch(regexMatch: Regex.Match): MavenDependency =
        MavenDependency(
          regexMatch.group(1), // group
          regexMatch.group(2), // artifact
          regexMatch.group(3)  // version
        )

      implicit val encoder: JsonEncoder[MavenDependency] =
        DeriveJsonEncoder.gen[MavenDependency]

      implicit val decoder: JsonDecoder[MavenDependency] =
        DeriveJsonDecoder.gen[MavenDependency]

    }

    object MavenDependencySerde {
      val key: Serde[Any, String] =
        Serde.string

      val value: Serde[Any, MavenDependency] =
        Serde.string.inmapM[Any, MavenDependency](s =>
          ZIO.fromEither(s.fromJson[MavenDependency])
            .mapError(e => new RuntimeException(e))
        )(r => ZIO.succeed(r.toJson))
    }

    case class InvalidDependency(line: String, parseError: String) extends Dependency

    /** Json codecs for the InvalidDependency class
      */
    object InvalidDependency {
      implicit val encoder: JsonEncoder[InvalidDependency] =
        DeriveJsonEncoder.gen[InvalidDependency]

      implicit val decoder: JsonDecoder[InvalidDependency] =
        DeriveJsonDecoder.gen[InvalidDependency]
    }

    object InvalidDependencySerde {
      val key: Serde[Any, String] =
        Serde.string

      val value: Serde[Any, InvalidDependency] =
        Serde.string.inmapM[Any, InvalidDependency](s =>
          ZIO.fromEither(s.fromJson[InvalidDependency])
            .mapError(e => new RuntimeException(e))
        )(r => ZIO.succeed(r.toJson))
    }

    case class MissingRemoteDependency(dep: MavenDependency, error: String) extends Dependency

    case class RemoteDependency(local: MavenDependency, remote: MavenDependency) extends Dependency
  }

  // alternative regex: (.*):(.*):(.*)
  // raw"(^[a-z][a-z0-9-_.]+):([a-z0-9-_.]+):([0-9]{1,2}\\.[0-9]{1,2}[0-9A-Za-z-.]*)".r
  private val pattern: Regex =
    "(^[^:A-Z]+):([^:A-Z]+):([^:a-z]+$)".r

  def getLinesFromFilename(filename: String): ZStream[Any, Throwable, String] =
    ZStream.fromIteratorScoped(
      ZIO.fromAutoCloseable(
        ZIO.attempt(scala.io.Source.fromFile(filename))
      ).map(_.getLines())
    )

  def parseDepLine(line: String): UIO[Dependency] = {
    def extract(line: String): UIO[Dependency] =
      line match {
        case pattern(g, a, v) => ZIO.succeed(MavenDependency(g, a, v))
        case _                => ZIO.succeed(InvalidDependency(line, "regex does not match"))
      }

    for {
      _      <- ZIO.log(s"parsing line: $line")
      result <- extract(line)
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
