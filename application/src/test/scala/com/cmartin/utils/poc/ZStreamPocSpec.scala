package com.cmartin.utils.poc

import com.cmartin.utils.poc.StreamBasedLogic._
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

  def createNowDate(): LocalDate =
    LocalDate.now()

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
    val amount = run(result)

    info(s"amount: $amount")

    amount.isBlank shouldBe false
  }

  it should "parallel process a stream from a dependency file" in {
    val filename = "src/test/resources/dep-list.log"
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