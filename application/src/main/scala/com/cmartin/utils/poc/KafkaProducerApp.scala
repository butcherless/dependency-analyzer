package com.cmartin.utils.poc

import zio.ZIOAppDefault
import zio._
import zio.kafka.producer.Producer
import zio.kafka.serde._
import zio.stream.{UStream, ZStream}

import java.util.UUID
import zio.kafka.producer.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord

object KafkaProducerApp
    extends ZIOAppDefault {

  val KAFKA_TOPIC                                             = "my-event-topic"
  val events: UStream[ProducerRecord[Long, KafkaPoc.MyEvent]] = ???
  val eventProducer                                           =
    events.via(Producer.produceAll(KafkaPoc.MyEventSerde.key, KafkaPoc.MyEventSerde.value))

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
