package com.cmartin.zio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wvlet.airframe.ulid.ULID
import zio.Runtime.{default => runtime}
import zio._
import zio.stream._

import java.time.LocalDate

class ZStreamPocSpec
    extends AnyFlatSpec
    with Matchers {

  behavior of "ZStreamPocSpec"

  case class Event(id: ULID, date: LocalDate, amount: Double)

  def buildEvent(date: LocalDate, amount: Double): Event =
    Event(ULID.newULID, date, amount)

  def sumAmount(amount: Double, event: Event): Double =
    amount + event.amount

  def roundAmount(amount: Double): Double =
    BigDecimal(amount).setScale(2, scala.math.BigDecimal.RoundingMode.HALF_DOWN).doubleValue

  def createNowDate() = LocalDate.now()

  def isNowDatePolicy(date: LocalDate): Boolean =
    date.isAfter(createNowDate())

  def calcAmountPerDay(annualAmount: Int, daysPerYear: Int): Double =
    annualAmount * daysPerYear / scala.math.pow(365, 2)

  def formatAmount(amount: Double): UIO[String] =
    ZIO.succeed(String.format("%.2f", roundAmount(amount)))

  it should "calculate sum" in {
    // GIVEN
    val initDate     = LocalDate.of(2021, 10, 11)
    val initAmount   = 0.0
    val annualAmount = 53_000
    val daysPerYear  = 33
    val amountPerDay = calcAmountPerDay(annualAmount, daysPerYear)

    // WHEN
    val calculateAmount =
      ZStream.iterate(initDate)(_.plusDays(1L))
        .map(date => buildEvent(date, amountPerDay))
        .takeUntil(e => isNowDatePolicy(e.date))
        // .tap(e => ZIO.log(e.toString()))
        .runFold(initAmount)(sumAmount)

    val result = for {
      amount          <- calculateAmount
      formattedAmount <- formatAmount(amount)
      _               <- ZIO.log(s"sum: $formattedAmount")
    } yield formattedAmount

    // THEN
    val amount = Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(result).getOrThrowFiberFailure()
    }

    info(s"amount: $amount")

    amount.isBlank shouldBe false
  }

  sealed trait DomainError
  object DomainError {
    case object EmptyLine                   extends DomainError
    case class InvalidLine(message: String) extends DomainError
  }

  sealed trait Line extends Product

  object Line {
    case class ValidLine(message: String) extends Line

    case class InvalidLine(message: String) extends Line
  }

  def calcLineLength(line: String) =
    ZIO.succeed(line.length)

//  def checkEmptyLine(line:String) =

  def validateLine(line: String) =
    if (line.nonEmpty)
      ZIO.succeed(Line.ValidLine(line))
    else
      ZIO.succeed(Line.InvalidLine("empty line"))

  it should "process string lines" in {
    val lines = List(
      "valid_line-1",
      "",
      "valid_line-2",
      "invalid_line-3",
      "valid_line-4",
      "invalid_line-5",
      "valid_line-6",
      "valid_line-7"
    )

    val program = ZStream
      .fromIterable(lines)
      .tap(line => ZIO.log(s"text: $line"))
      .mapZIO(validateLine)
      .tap(line => ZIO.log(s"line: $line"))
      .runDrain

    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }
  }

  sealed trait Dependency

  object Dependency {
    case class LocalDependency(g: String, a: String, v: String)    extends Dependency
    case class InvalidDependency(line: String, parseError: String) extends Dependency
  }

  import Dependency._
  def parse(line: String): Dependency   =
    line match {
      case "valid" => LocalDependency("g", "a", "v")
      case _       => InvalidDependency(line, "parse error")
    }
  def isValid(dep: Dependency): Boolean = ???

  def processInvalid(dep: Dependency): Unit = ???
  def processValid(dep: Dependency): Unit   = ???

  it should "TODO:" in {
    val valid               = "valid"
    val invalid             = "invalid"
    val lines: List[String] = List(valid, valid, invalid, valid, invalid, valid, valid)
    val depStream           =
      ZStream
        .fromIterable(lines)
        .map(parse)
    val program             =
      depStream
        .debug
        .partition(isValid)
        .flatMap(streams =>
          ZIO.succeed(streams._1.map(processInvalid) merge streams._2.map(processValid))
        ) // .flatMap(_.runDrain)

    val x = ZIO.scoped(program).flatMap(_.runDrain)

    val result = Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(x).getOrThrowFiberFailure()
    }
  }

}
