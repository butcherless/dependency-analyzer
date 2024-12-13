package dev.cmartin.learn

import dev.cmartin.learn.DomainModel.{Currency, CurrencySymbol}
import zio.json.*

object JsonCodecs {

  object CurrencySymbol {
    implicit val encoder: JsonEncoder[CurrencySymbol] =
      DeriveJsonEncoder.gen[CurrencySymbol]

    implicit val decoder: JsonDecoder[CurrencySymbol] =
      DeriveJsonDecoder.gen[CurrencySymbol]

  }

  object Currency {
    import CurrencySymbol.*
    implicit val encoder: JsonEncoder[Currency] =
      DeriveJsonEncoder.gen[Currency]

    implicit val decoder: JsonDecoder[Currency] =
      DeriveJsonDecoder.gen[Currency]

  }
}
