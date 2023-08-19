package com.cmartin.utils.poc

import java.util.UUID
import java.time.Instant
import zio.json._
import zio.kafka.serde.Serde
import zio.ZIO

object KafkaPoc {

  case class MyEvent(id: UUID, number: Long, timestamp: Instant)

  object MyEvent {
    implicit val encoder: JsonEncoder[MyEvent] =
      DeriveJsonEncoder.gen[MyEvent]

    implicit val decoder: JsonDecoder[MyEvent] =
      DeriveJsonDecoder.gen[MyEvent]
  }

  object MyEventSerde {
    val key: Serde[Any, Long] =
      Serde.long

    val value: Serde[Any, MyEvent] =
      Serde.string.inmapM[Any, MyEvent](s =>
        ZIO.fromEither(s.fromJson[MyEvent])
          .mapError(e => new RuntimeException(e))
      )(r => ZIO.succeed(r.toJson))

  }

}
