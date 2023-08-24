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

  val KAFKA_TOPIC = "dependency-line-topic"

  def buildValue(projectName: String, line: String, time: Instant): DependencyLine =
    KafkaPoc.DependencyLine(projectName, line, time)

  def buildRecord[K, V](topic: String, key: K, depLine: V) =
    new ProducerRecord(
      topic,
      key,
      depLine
    )

  def log10PercentMetadata(metadata: RecordMetadata): UIO[Unit] =
    if (metadata.timestamp() % 100 < 5)
      ZIO.log(s"[topic=${metadata.topic},partition=${metadata.partition},offset=${metadata.offset}]")
    else ZIO.unit

  val filename     = "application/src/test/resources/dep-list.log"
  val PROJECT_NAME = "dependency-analyzer"
  val ZIO_LINE     = "dev.zio:zio:2.0.16"
  val lineStream   =
    StreamBasedLogic.getLinesFromFilename(filename)
      .tap(line => ZIO.log(s"line $line"))
      .mapZIO(ZIO.succeed(_) zip Clock.currentDateTime)
      .map { case (line, time) =>
        buildRecord(KAFKA_TOPIC, PROJECT_NAME, buildValue(PROJECT_NAME, line, time.toInstant))
      }
      .tap(record => ZIO.log(s"record $record"))
      .via(Producer.produceAll(DependencyLineSerde.key, DependencyLineSerde.value))
      .tap(log10PercentMetadata)

  val EVENT_COUNT: Int        = 100
  val bootstrapServer: String = "localhost:29092"
  val dummyProducer           = // : ZStream[Producer, Throwable, Nothing] =
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
  // .drain

  def producerLayer =
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
