package com.cmartin.utils.poc

import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIOAppDefault
import zio._
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings
import zio.stream.ZStream

import java.time.Instant
import KafkaPoc._
import com.cmartin.utils.poc.StreamBasedLogic.Dependency.{
  InvalidDependency,
  InvalidDependencySerde,
  MavenDependency,
  MavenDependencySerde
}
import com.cmartin.utils.poc.StreamBasedLogic._
import org.apache.kafka.clients.producer.RecordMetadata

object KafkaProducerApp
    extends ZIOAppDefault {

  val bootstrapServer: String = "localhost:29092"
  val DEPENDENCY_LINE_TOPIC   = "dependency-line-topic"
  val INVALID_LINE_TOPIC      = "invalid-line-topic"
  val filename                = "application/src/test/resources/dep-list.log"
  val PROJECT_NAME            = "dependency-analyzer"
  val ZIO_LINE                = "dev.zio:zio:2.0.16"
  val EVENT_COUNT: Int        = 100

  def buildValue(projectName: String, line: String, time: Instant): DependencyLine =
    KafkaPoc.DependencyLine(projectName, line, time)

  def buildRecord[K, V](topic: String, key: K, value: V) =
    new ProducerRecord(
      topic,
      key,
      value
    )

  def buildLogLine(metadata: RecordMetadata): String =
    s"[topic=${metadata.topic},partition=${metadata.partition},offset=${metadata.offset}]"

  def logMetadata(metadata: RecordMetadata)(probability: Double = 0.05): ZIO[Any, Nothing, Unit] =
    for {
      number <- Random.nextDouble
      _      <- ZIO.when(number <= probability)(
                  ZIO.log(buildLogLine(metadata))
                )
    } yield ()

  private def processMavenDependency(dep: MavenDependency) =
    ZIO.log(s"valid dependency: $dep") *>
      ZIO.succeed(buildRecord(DEPENDENCY_LINE_TOPIC, PROJECT_NAME, dep))

  private def processInvalidDependency(dep: InvalidDependency) =
    ZIO.log(s"invalid dependency: $dep") *>
      ZIO.succeed(buildRecord(INVALID_LINE_TOPIC, PROJECT_NAME, dep))

  private val mainProgram =
    StreamBasedLogic.getLinesFromFilename(filename)
      .mapZIOPar(2)(parseDepLine)
      .partition(isValidDep)
      .flatMap { case (validStream, invalidStream) =>
        ZStream.mergeAll(2)(
          validStream.collectType[MavenDependency]
            .mapZIO(processMavenDependency)
            .tap(record => ZIO.log(s"$record"))
            .via(Producer.produceAll(MavenDependencySerde.key, MavenDependencySerde.value)),
          invalidStream.collectType[InvalidDependency]
            .mapZIO(processInvalidDependency)
            .tap(record => ZIO.log(s"$record"))
            .via(Producer.produceAll(InvalidDependencySerde.key, InvalidDependencySerde.value))
        ).runDrain
      }

  /*
      .mapZIO(ZIO.succeed(_) zip Clock.instant)
      .map { case (line, time) =>
        buildRecord(DEPENDENCY_LINE_TOPIC, PROJECT_NAME, buildValue(PROJECT_NAME, line, time))
      }
      .tap(record => ZIO.log(s"$record"))
      .via(Producer.produceAll(DependencyLineSerde.key, DependencyLineSerde.value))
      .tap(logMetadata(_)())

   */

  // TODO: stream for researching
  val dummyProducer         =
    ZStream
      .fromSchedule(Schedule.recurs(EVENT_COUNT) && Schedule.fixed(250.milliseconds))
      .mapZIO(_ => Clock.currentDateTime)
      .map(time => (PROJECT_NAME, time))
      .map { case (projectName, time) =>
        buildRecord(DEPENDENCY_LINE_TOPIC, projectName, buildValue(projectName, ZIO_LINE, time.toInstant))
      }
      .tap(record => ZIO.log(s"record $record"))
      .via(Producer.produceAll(DependencyLineSerde.key, DependencyLineSerde.value))
      .tap(logMetadata(_)())
  private def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List(bootstrapServer))
      )
    )

  def run =
    for {
      _ <- ZIO.log("kafka producer")
      _ <- ZIO.scoped(mainProgram)
             .provide(producerLayer)
      // _ <- res1.runDrain.provide(producerLayer)
      // a <- dummyProducer.runDrain.provide(producerLayer)
      // .provide(producerLayer)
    } yield ()

}
