package com.cmartin.utils.poc

import com.cmartin.utils.poc.StreamBasedLogic.Dependency.InvalidDependencySerde
import com.cmartin.utils.poc.StreamBasedLogic.logObject
import zio.kafka.consumer.{Consumer, ConsumerSettings, Subscription}
import zio.stream.ZStream
import zio.{RIO, Scope, ZIOAppArgs, ZIOAppDefault, ZLayer}

object KafkaInvalidLineConsumerApp
    extends ZIOAppDefault {

  // TODO application configuration
  private val BOOSTRAP_SERVERS: List[String] = List("localhost:29092")
  private val INVALID_LINE_TOPIC             = "invalid-line-topic"

  private val consumerLayer: ZLayer[Any, Throwable, Consumer] =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(BOOSTRAP_SERVERS)
          .withGroupId("invalid-line-kafka-app")
      )
    )

  private val mainProgram: ZStream[Consumer, Throwable, Unit] =
    Consumer
      .plainStream(Subscription.topics(INVALID_LINE_TOPIC), InvalidDependencySerde.key, InvalidDependencySerde.value)
      .tap(logObject)
      .map(record => record.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapZIO(batch => batch.commit)

  def run: RIO[ZIOAppArgs with Scope, Unit] =
    for {
      _ <- logObject("kafka invalid line consumer application")
      _ <- mainProgram.runDrain
             .provide(consumerLayer)

    } yield ()

}

/*
val c: ZStream[Consumer, Throwable, Nothing] =
      Consumer
        .plainStream(Subscription.topics(KAFKA_TOPIC), KafkaSerde.key, KafkaSerde.value)
        .tap(e => Console.printLine(e.value))
        .map(_.offset)
        .aggregateAsync(Consumer.offsetBatches)
        .mapZIO(_.commit)
        .drain
 */
