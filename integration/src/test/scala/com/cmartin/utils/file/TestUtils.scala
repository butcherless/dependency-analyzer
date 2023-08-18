package com.cmartin.utils.file

import zio.{Unsafe, ZIO}
import zio.Runtime.{default => runtime}

object TestUtils {

  def run[E, A](program: ZIO[Any, E, A]) =
    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }

}
