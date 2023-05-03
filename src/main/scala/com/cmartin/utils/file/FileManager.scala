package com.cmartin.utils.file

import Utils._
import com.cmartin.utils.domain.{IOManager, Model}
import Model._
import com.cmartin.utils.domain.Model.DomainError.FileIOError
import zio._

import scala.io.{BufferedSource, Source}

final case class FileManager()
    extends IOManager {

  override def getLinesFromFile(filename: String): IO[DomainError, List[String]] =
    ZIO.scoped {
      scopedFile(filename).flatMap { file =>
        ZIO.logInfo(s"reading from file: $filename") *>
          ZIO.attempt(file.getLines().toList)
      }.orElseFail(FileIOError(s"${Model.OPEN_FILE_ERROR}: $filename"))
    }
  override def logWrongDependencies(errors: Iterable[DomainError]): UIO[Unit] =
    ZIO.foreachDiscard(errors)(e => ZIO.logInfo(s"invalid dependency: $e"))

  override def filterUpgraded(collection: Iterable[GavPair]): UIO[Iterable[String]] =
    ZIO.succeed(
      collection
        .filter(_.hasNewVersion)
        .map(formatChanges)
    )

  /*
    H E L P E R S
   */

  private def scopedFile(filename: String): RIO[Scope, BufferedSource] =
    ZIO.fromAutoCloseable(ZIO.attempt(Source.fromFile(filename)))

  private def formatChanges(pair: Model.GavPair): String =
    s"${pair.local.formatShort} ${colourGreen("=>")} ${colourYellow(pair.remote.version)}"

}

object FileManager {
  val layer: ULayer[IOManager] =
    ZLayer.succeed(FileManager())
}
