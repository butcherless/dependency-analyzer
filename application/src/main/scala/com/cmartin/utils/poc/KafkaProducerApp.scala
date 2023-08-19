package com.cmartin.utils.poc

import zio.ZIOAppDefault
import zio._
import zio.kafka.producer.Producer
import zio.kafka.serde._
import zio.stream.ZStream

import java.util.UUID
import zio.kafka.producer.ProducerSettings

object KafkaProducerApp
    extends ZIOAppDefault {

  def buildValue(value: String) =
    s"""{
        "value" : "$value",
        "id" : "${UUID.randomUUID()}"
    }""".stripMargin

  val producer: ZStream[Producer, Throwable, Nothing] =
    ZStream
      .repeatZIO(Random.nextIntBetween(0, Int.MaxValue))
      .schedule(Schedule.fixed(250.milliseconds))
      .mapZIO { random =>
        Producer.produce[Any, Long, String](
          topic = "random",
          key = random % 4,
          value = buildValue(random.toString),
          keySerializer = Serde.long,
          valueSerializer = Serde.string
        )
      }
      .tap(a => ZIO.log(s"element: $a"))
      .drain

  def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List("localhost:29092"))
      )
    )

  def run =
    for {
      _ <- ZIO.log("kafka producer")
      a <- producer.runDrain.provide(producerLayer)
    } yield ()

}
