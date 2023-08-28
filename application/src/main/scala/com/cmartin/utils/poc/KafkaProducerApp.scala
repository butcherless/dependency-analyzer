package com.cmartin.utils.poc

import com.cmartin.utils.poc.StreamBasedLogic.Dependency.{InvalidDependency, InvalidDependencySerde, MavenDependency, MavenDependencySerde}
import com.cmartin.utils.poc.StreamBasedLogic._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.stream.ZStream
import zio.{ZIOAppDefault, _}

object KafkaProducerApp
    extends ZIOAppDefault {

  private val bootstrapServer: String = "localhost:29092"
  private val DEPENDENCY_LINE_TOPIC   = "dependency-line-topic"
  private val INVALID_LINE_TOPIC      = "invalid-line-topic"
  private val filename                = "application/src/test/resources/dep-list.log"
  private val PROJECT_NAME            = "dependency-analyzer"

  private val mainProgram =
    StreamBasedLogic.getLinesFromFilename(filename)
      .mapZIOPar(2)(parseDepLine)
      .partition(isValidDep)
      .flatMap { case (validStream, invalidStream) =>
        ZStream.mergeAll(2)(
          // valid line case
          validStream.collectType[MavenDependency]
            .mapZIO(dep =>
              processMavenDependency(MavenDependencyRequest(DEPENDENCY_LINE_TOPIC, PROJECT_NAME, dep))
            )
            .tap(logObject)
            .via(Producer.produceAll(MavenDependencySerde.key, MavenDependencySerde.value))
            .tap(logMetadata(_)(0.10)),
          // invalid line case
          invalidStream.collectType[InvalidDependency]
            .mapZIO(dep =>
              processInvalidDependency(InvalidDependencyRequest(INVALID_LINE_TOPIC, PROJECT_NAME, dep))
            )
            .tap(logObject)
            .via(Producer.produceAll(InvalidDependencySerde.key, InvalidDependencySerde.value))
        ).runDrain
      }

  private def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List(bootstrapServer))
      )
    )

  def run: RIO[ZIOAppArgs with Scope, Unit] =
    for {
      _ <- ZIO.log("kafka producer application")
      _ <- ZIO.scoped(mainProgram)
             .provide(producerLayer)
    } yield ()

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

/*
// TODO: stream for researching

private val ZIO_LINE                = "dev.zio:zio:2.0.16"
private val EVENT_COUNT: Int        = 100

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

private def buildValue(projectName: String, line: String, time: Instant): DependencyLine =
  KafkaPoc.DependencyLine(projectName, line, time)

 */
