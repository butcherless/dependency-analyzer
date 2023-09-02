package com.cmartin.utils.poc

import com.cmartin.utils.TestUtils
import just.semver.SemVer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.{IO, UIO, ZIO}

import scala.util.matching.Regex

class SemverValidatorSpec extends AnyFlatSpec
    with Matchers {

  behavior of "ZStreamPocSpec"
  private val pattern: Regex =
    "(^[^:A-Z]+):([^:A-Z]+):([^:]+$)".r

  val nettyLine = "io.netty:netty-buffer:4.1.95.Final"
  val slf4jLine = "org.slf4j:slf4j-api:2.0.0-alpha1"

  def extractVersion(line: String): IO[String, String] =
    line match {
      case pattern(_, _, v) => ZIO.succeed(v)
      case _                => ZIO.fail("invalid version")
    }

  def validate(version: String): UIO[Boolean] =
    SemVer.parse(version).fold(
      _ => ZIO.succeed(false),
      _ => ZIO.succeed(true)
    )

  it should s"validate $nettyLine" in {
    val program = for {
      version <- extractVersion(nettyLine)
      _       <- zio.Console.printLine(s"$version")
      result  <- validate(version)
    } yield result

    TestUtils.run(program) shouldBe false
  }

  it should s"validate $slf4jLine" in {
    val program = for {
      version <- extractVersion(slf4jLine)
      _       <- zio.Console.printLine(s"$version")
      result  <- validate(version)
    } yield result

    TestUtils.run(program) shouldBe true
  }

}
