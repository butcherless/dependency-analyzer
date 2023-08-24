package com.cmartin.utils.poc

import java.util.UUID
import java.time.Instant
import zio.json._
import zio.kafka.serde.Serde
import zio.ZIO

object KafkaPoc {

  case class DependencyLine(project: String, text: String, readTime: Instant)

  object DependencyLine {
    implicit val encoder: JsonEncoder[DependencyLine] =
      DeriveJsonEncoder.gen[DependencyLine]

    implicit val decoder: JsonDecoder[DependencyLine] =
      DeriveJsonDecoder.gen[DependencyLine]
  }

  object DependencyLineSerde {
    val key: Serde[Any, String] =
      Serde.string

    val value: Serde[Any, DependencyLine] =
      Serde.string.inmapM[Any, DependencyLine](s =>
        ZIO.fromEither(s.fromJson[DependencyLine])
          .mapError(e => new RuntimeException(e))
      )(r => ZIO.succeed(r.toJson))

  }

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
