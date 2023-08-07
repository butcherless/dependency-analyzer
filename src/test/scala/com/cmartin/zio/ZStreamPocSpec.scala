package com.cmartin.zio

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wvlet.airframe.ulid.ULID
import zio.Runtime.{default => runtime}
import zio._
import zio.stream._

import java.time.LocalDate
import scala.util.matching.Regex

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
    val amount = run(result)

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

    run(program)
  }

  sealed trait Dependency

  object Dependency {
    case class MavenDependency(g: String, a: String, v: String) extends Dependency
    object MavenDependency {
      def fromRegexMatch(regexMatch: Regex.Match): MavenDependency =
        MavenDependency(
          regexMatch.group(1), // group
          regexMatch.group(2), // artifact
          regexMatch.group(3)  // version
        )

    }
    case class InvalidDependency(line: String, parseError: String)               extends Dependency
    case class MissingRemoteDependency(dep: MavenDependency, error: String)      extends Dependency
    case class RemoteDependency(local: MavenDependency, remote: MavenDependency) extends Dependency
  }

  import Dependency._
  def parse(line: String): Dependency   =
    line match {
      case "valid" => MavenDependency("g", "a", "v")
      case _       => InvalidDependency(line, "parse error")
    }
  def isValid(dep: Dependency): Boolean = ???

  def processInvalid(dep: Dependency): Unit = ???
  def processValid(dep: Dependency): Unit   = ???

  it should "TODO: A" in {
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
          ZIO.succeed(streams._1.map(processValid) merge streams._2.map(processInvalid))
        ) // .flatMap(_.runDrain)

    val x = ZIO.scoped(program).flatMap(_.runDrain)

    val result = run(x)
  }

  def processLine(line: String): IO[String, Dependency]          = ???
  def processDependency(dep: Dependency): IO[String, Dependency] = ???
  def process2Invalid(dep: Dependency): IO[String, Dependency]   = ???
  def process2Valid(dep: Dependency): IO[String, Dependency]     =
    dep match {
      case MavenDependency(g, a, v) => ZIO.succeed(MavenDependency(g, a, v))
      case _                        => ZIO.fail("This type of dependency is not expected")
    }

  def parseLine(line: String): IO[String, Dependency] = ???

  // TODO
  ignore should "TODO: B" in {
    val lineStream: ZStream[Any, Nothing, String] = ???

    val parsedLines         = lineStream.mapZIO(parseLine)
    val dependencyPartition = parsedLines.partition(isValid)

    val x12 = dependencyPartition.flatMap { case (validStream, invalidStream) =>
      val ok = validStream.mapZIO(process2Valid)
      val ko = invalidStream.mapZIO(process2Invalid)

      ZIO.succeed(ok merge ko)
    }
    val xx  = x12.flatMap(_.run(ZSink.drain))

    val r: ZStream[Any, String, Dependency] = lineStream
      .mapZIO(processLine)
      .mapZIO(processDependency)

    val dependencies: ZStream[Any, Nothing, Dependency] = lineStream.map(parse)

    val x1: ZIO[Any, Nothing, (ZStream[Any, Nothing, Dependency], ZStream[Any, Nothing, Dependency])] =
      ZIO.scoped(dependencies.partition(isValid))

    val validDeps: ZStream[Any, Nothing, Dependency]   = ???
    val invalidDeps: ZStream[Any, Nothing, Dependency] = ???
    val remoteDeps: ZStream[Any, Nothing, Dependency]  = ???

    val x2 = invalidDeps merge validDeps merge remoteDeps

    val s1: ZStream[Any, Nothing, String] = ZStream("1", "2", "3")
    val s2: ZStream[Any, Nothing, Double] = ZStream(4.1, 5.3, 6.2)

    val merged: ZStream[Any, Nothing, Int] =
      s1.mergeWith(s2)(_.toInt, _.toInt)

    val mergedStream: ZStream[Any, Nothing, Dependency] =
      ZStream.mergeAll(3)(validDeps, invalidDeps, remoteDeps)
  }

  private val pattern: Regex =
    "(^[a-z][a-z0-9-_.]+):([a-z0-9-_.]+):([0-9]{1,2}\\.[0-9]{1,2}[0-9A-Za-z-.]*)".r

  private def parseDepLine(line: String): Task[Dependency] = {
    def extractDependency(iterator: Iterator[Regex.Match]) =
      if (iterator.hasNext) ZIO.succeed(MavenDependency.fromRegexMatch(iterator.next()))
      else ZIO.succeed(InvalidDependency(line, "regex does not match"))

    for {
      _             <- ZIO.logDebug(s"parsing line: $line")
      matchIterator <- ZIO.succeed(pattern.findAllMatchIn(line))
      result        <- extractDependency(matchIterator)
    } yield result
  }

  it should "process a stream from a dependency file" in {
    val filename = "src/test/resources/dep-list.log"
    val program =
      // : ZStream[Any, Throwable, Dependency] =
      ZStream.fromIteratorScoped(
        ZIO.fromAutoCloseable(
          ZIO.attempt(scala.io.Source.fromFile(filename))
        ).map(_.getLines())
      ).tap(line => ZIO.log(s"line: $line"))
        .mapZIO(parseDepLine)
        // .tap(dep => ZIO.log(s"dependency: $dep"))
        .partition(isValidDep).flatMap { case (validStream, invalidStream) =>
          ZStream.mergeAll(2)(
            validStream.tap(dep => ZIO.log(s"valid dependency: $dep")),
            invalidStream.tap(dep => ZIO.log(s"invalid dependency: $dep"))
          )
            .runDrain
        }

    run(ZIO.scoped(program))
  }

  private def isValidDep(dep: Dependency) =
    dep match {
      case MavenDependency(_, _, _) => true
      case _                        => false
    }

  private def run[E, A](program: ZIO[Any, E, A]) =
    Unsafe.unsafe { implicit u =>
      runtime.unsafe.run(program).getOrThrowFiberFailure()
    }

  object Poc {
    sealed trait Base             extends Product with Serializable
    case class Valid(a: Long)     extends Base
    case class Invalid(a: String) extends Base

    // partition function
    def isValid(base: Base): Boolean = base match {
      case Valid(_)   => true
      case Invalid(_) => false
    }
    def parse(s: String): Base       = ???

    val lines: List[String] = ???

    val s2: ZStream[Any, Nothing, Base] = ???

    def processValid(base: Base): Valid = base match {
      case Valid(a) => Valid(a)
      case _        => throw new RuntimeException("invalid type") // TODO fail with domain error
    }

    val s3                                  = s2.map(processValid)
    val s4                                  = s2.map(processInvalid)
    def processInvalid(base: Base): Invalid = ???

    // TODO manage error channel
    val s1 = for {
      tuple   <- ZStream.fromIterable(lines).map(parse).partition(isValid)
      (vs, is) = tuple
      count   <- ZStream.mergeAll(2)(
                   vs.map(processValid),
                   is.map(processValid)
                 ).run(ZSink.count)
    } yield count

    val result = ZIO.scoped(s1)
  }
}
