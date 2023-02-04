package com.cmartin.zio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.Runtime.{default => runtime}
import zio._
import zio.stream._
import java.time.LocalDate
import wvlet.airframe.ulid.ULID

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
        .tap(e => ZIO.log(e.toString()))
        .runFold(initAmount)(sumAmount)

    val result = for {
      amount          <- calculateAmount
      formattedAmount <- ZIO.succeed(String.format("%.2f", roundAmount(amount)))
      _               <- ZIO.log(s"sum: $formattedAmount")
    } yield ()

    // THEN
    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(result).getOrThrowFiberFailure()
    }

  }

}
