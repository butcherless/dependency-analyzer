package com.cmartin.utils.poc

import org.apache.kafka.clients.producer.ProducerRecord
import zio.ZIOAppDefault
import zio._
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings
import zio.stream.ZStream

import java.time.Instant

import KafkaPoc._
import org.apache.kafka.clients.producer.RecordMetadata

object KafkaProducerApp
    extends ZIOAppDefault {

  val bootstrapServer: String = "localhost:29092"
  val KAFKA_TOPIC             = "dependency-line-topic"
  val filename                = "application/src/test/resources/dep-list.log"
  val PROJECT_NAME            = "dependency-analyzer"
  val ZIO_LINE                = "dev.zio:zio:2.0.16"

  def buildValue(projectName: String, line: String, time: Instant): DependencyLine =
    KafkaPoc.DependencyLine(projectName, line, time)

  def buildRecord[K, V](topic: String, key: K, value: V) =
    new ProducerRecord(
      topic,
      key,
      value
    )

  def log10PercentMetadata(metadata: RecordMetadata): UIO[Unit] =
    if (metadata.timestamp() % 100 < 5)
      ZIO.log(s"[topic=${metadata.topic},partition=${metadata.partition},offset=${metadata.offset}]")
    else ZIO.unit

  val lineStream =
    StreamBasedLogic.getLinesFromFilename(filename)
      .tap(line => ZIO.log(s"line $line"))
      .mapZIO(ZIO.succeed(_) zip Clock.instant)
      .map { case (line, time) =>
        buildRecord(KAFKA_TOPIC, PROJECT_NAME, buildValue(PROJECT_NAME, line, time))
      }
      .tap(record => ZIO.log(s"$record"))
      .via(Producer.produceAll(DependencyLineSerde.key, DependencyLineSerde.value))
      .tap(log10PercentMetadata)

  val EVENT_COUNT: Int      = 100
  val dummyProducer         =
    ZStream
      .fromSchedule(Schedule.recurs(EVENT_COUNT) && Schedule.fixed(250.milliseconds))
      .mapZIO(_ => Clock.currentDateTime)
      .map(time => (PROJECT_NAME, time))
      .map { case (projectName, time) =>
        buildRecord(KAFKA_TOPIC, projectName, buildValue(projectName, ZIO_LINE, time.toInstant))
      }
      .tap(record => ZIO.log(s"record $record"))
      .via(Producer.produceAll(DependencyLineSerde.key, DependencyLineSerde.value))
      .tap(log10PercentMetadata)
  private def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List(bootstrapServer))
      )
    )

  def run =
    for {
      _ <- ZIO.log("kafka producer")
      // a <- dummyProducer.runDrain.provide(producerLayer)
      _ <- lineStream.runDrain.provide(producerLayer)
    } yield ()

}
