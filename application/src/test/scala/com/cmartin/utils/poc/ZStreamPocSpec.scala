package com.cmartin.utils.poc

import com.cmartin.utils.TestUtils
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wvlet.airframe.ulid.ULID
import zio._
import zio.stream._

import java.nio.file.{Files, Paths}
import java.time.LocalDate
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex
import java.text.NumberFormat
import java.util.Locale

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
    // ZIO.succeed(String.format("%.2f", roundAmount(amount)))
    ZIO.succeed(formatCurrency(amount))

  lazy val locale                            = Locale.GERMAN
  lazy val formatter                         = NumberFormat.getCurrencyInstance(locale)
  def formatCurrency(amount: Double): String =
    formatter.format(amount)

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
    val amount: String = TestUtils.run(result)

    info(s"amount: $amount")

    amount.isBlank shouldBe false
    amount should contain allOf (',', '.')
  }

  it should "generate elements from recurs & fixed schedule" in {
    val EVENT_COUNT = 10
    val schedule    = Schedule.recurs(EVENT_COUNT) >>> Schedule.fixed(20.milliseconds)
    val stream      =
      ZStream.fromSchedule(schedule)
        .tap(a => Console.printLine(s"element: $a"))

    TestUtils.run(stream.runDrain)
  }

  val moduleRegex: Regex = raw"^\.{1,2}/(.*)/src/.*".r
  val path               = "../application/src/main/scala/com/cmartin/utils/http/ZioHttpManager.scala"
  val expectedSet        = Set("application", "integration", "scraper")

  it should "read scala files from project using zio streams" in {
    val fileExtension = ".scala"
    val projectPath   = Paths.get("..");

    val program =
      ZStream
        .fromJavaStream(Files.walk(projectPath))
        .filter(path => Files.isRegularFile(path))
        .map(_.toString)
        .filter(path => path.endsWith(fileExtension))
        .collect { case moduleRegex(module) => module }
        .runFold(Set.empty[String])((set, module) => set + module)

    val modules = TestUtils.run(program)

    info(s"project modules: $modules")

    modules shouldBe expectedSet
  }

  it should "read scala files from project using scala streams" in {
    val fileExtension = ".scala"
    val path          = Paths.get("..");

    val modules =
      Files.list(path)
        .iterator()
        .asScala
        .filter(p => Files.isDirectory(p) && !Files.isHidden(p))
        .flatMap { dir =>
          Files.walk(dir)
            .iterator().asScala
            .map(_.toString)
            .find(path => path.endsWith(fileExtension))
            .collect { case moduleRegex(module) => module }

        }.toSet

    info(s"modules: $modules")
    modules shouldBe expectedSet
  }
}
