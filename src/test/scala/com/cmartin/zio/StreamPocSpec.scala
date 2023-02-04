package com.cmartin.zio

import com.cmartin.utils.domain.IOManager
import com.cmartin.utils.file.FileManager
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.Runtime.{default => runtime}
import zio._
import scala.io.{BufferedSource, Source}
import zio.stream._
import com.cmartin.utils.domain.Model

class StreamPocSpec
    extends AnyFlatSpec
    with Matchers {

  behavior of "StreamPoc"

  it should "read the file lines" in {
    // GIVEN
    val filename = "src/test/resources/dep-list.log"

    // val program = ZIO.fromAutoCloseable(ZIO.attempt(Source.fromFile(filename)))

    val lineStream: ZStream[Any, Throwable, String] =
      ZStream.fromIteratorScoped(
        ZIO.fromAutoCloseable(
          ZIO.attempt(Source.fromFile(filename))
        ).map(_.getLines())
      )

    val s2 = lineStream.map { s =>
      Model.Gav("todo-group", "todo-artifact", s"todo-version:${s.length}")
    }

    val s3 = lineStream.partition(_.contains(""))
      .map { case (a, b) =>
        (
          a.map(_.length()),
          b.map(_ => "")
        )
      }

    val program = s2.foreach(l => ZIO.log(s"line: $l"))

    // WHEN

    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }
    // THEN

  }

}
