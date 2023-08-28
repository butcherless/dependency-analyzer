package com.cmartin.utils

import zio.{Unsafe, ZIO}
import zio.Runtime.{default => runtime}

object TestUtils {

  def run[E, A](program: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }

}
