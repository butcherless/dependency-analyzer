package dev.cmartin.learn

import zio.json.*
import dev.cmartin.learn.DomainModel.Currency

object JsonCodecs {
  object Currency {

    implicit val encoder: JsonEncoder[Currency] =
      DeriveJsonEncoder.gen[Currency]

    implicit val decoder: JsonDecoder[Currency] =
      DeriveJsonDecoder.gen[Currency]

  }
}
