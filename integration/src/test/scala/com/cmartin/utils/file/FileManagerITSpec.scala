package com.cmartin.utils.file

import com.cmartin.utils.domain.Model.DomainError.FileIOError
import com.cmartin.utils.domain.{IOManager, Model}
import com.cmartin.utils.file.TestUtils.{run => unsafeRun}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FileManagerITSpec
    extends AnyFlatSpec
    with Matchers {

  behavior of "FileManager"

  it should "retrieve a sequence of text lines from a file" in {
    val filename = "integration/src/test/resources/dependency-list.txt"
    val program  = IOManager.getLinesFromFile(filename)
      .provide(FileManager.layer)

    val lines = unsafeRun(program)

    lines.nonEmpty shouldBe true
    lines shouldBe List("line-1", "line-2")
  }

  it should "fail for missing file" in {
    val filename = "missing-file.txt"
    val program  = IOManager.getLinesFromFile(filename)
      .provide(FileManager.layer)

    val lines =
      unsafeRun(program.either)

    lines shouldBe Left(FileIOError(s"${Model.OPEN_FILE_ERROR}: $filename"))
  }

}
