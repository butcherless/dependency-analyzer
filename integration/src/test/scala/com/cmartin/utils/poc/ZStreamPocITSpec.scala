package com.cmartin.utils.poc

import com.cmartin.utils.poc.StreamBasedLogic.{getLinesFromFilename, isValidDep, parseDepLine, processStreams}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.{Unsafe, ZIO}
import zio.Runtime.{default => runtime}

class ZStreamPocITSpec
    extends AnyFlatSpec
    with Matchers {

  behavior of "ZStreamPocSpec"

  it should "parallel process a stream from a dependency file" in {
    val filename = "integration/src/test/resources/dep-list.log"
    val program  =
      getLinesFromFilename(filename)
        .mapZIOPar(2)(parseDepLine)
        .partition(isValidDep)
        .flatMap { case (validStream, invalidStream) =>
          processStreams(validStream, invalidStream)
            .runDrain
        }

    run(ZIO.scoped(program))
  }

  private def run[E, A](program: ZIO[Any, E, A]) =
    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }

}
