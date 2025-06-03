package com.cmartin.utils.poc.filesystem

import zio.*
import scala.util.matching.Regex
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.io.IOException
import zio.stream.*

object JavaFileFinder
    extends ZIOAppDefault {

  // Regex to match java files
  val javaFileRegex: Regex = """.*\.java$""".r
  val path: Path           = Paths.get("/Users/cmartin/projects/inditex/iop")

  val javaFiles = searchJavaFiles(path, javaFileRegex)
    .map(_.toAbsolutePath)
    .filter(p => Files.isRegularFile(p) && javaFileRegex.matches(p.toString))
    .foreach(file => Console.printLine(file.toString))

  override def run =
    for {
      _ <- ZIO.log("searching java files")
      _ <- javaFiles
    } yield ()

  val javaPathStream: java.util.stream.Stream[Path] =
    Files.walk(path)

  val pathStream: ZStream[Any, IOException, Path] =
    ZStream
      .fromJavaStream(javaPathStream)
      .refineToOrDie[IOException]

  def searchJavaFiles(
      rootDir: Path,
      pattern: Regex = javaFileRegex
  ): ZStream[Any, IOException, Path] =
    ZStream
      .fromJavaStream(javaPathStream)
      .refineToOrDie[IOException]

  def getFileSize(file: Path): IO[IOException, Long] =
    ZIO
      .attempt(Files.size(file))
      .refineToOrDie[IOException]

  def getFileSizeWithStream(file: Path): IO[IOException, Long] =
    ZStream
      .fromPath(file)
      .runFold(0L)((acc, _) => acc + 1)
      .refineToOrDie[IOException]
}
