package com.cmartin.utils

import zio.Runtime.{default => runtime}
import zio.{Unsafe, ZIO}

object TestUtils {

  def run[E, A](program: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }

}
