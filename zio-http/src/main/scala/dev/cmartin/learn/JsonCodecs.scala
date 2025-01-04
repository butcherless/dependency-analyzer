package dev.cmartin.learn

import dev.cmartin.learn.DomainModel.{Currency, CurrencySymbol}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object JsonCodecs {

  object CurrencySymbol {

    /** Implicit JSON encoder for CurrencySymbol.
      */
    implicit val encoder: JsonEncoder[CurrencySymbol] =
      DeriveJsonEncoder.gen[CurrencySymbol]

    /** Implicit JSON decoder for CurrencySymbol.
      */
    implicit val decoder: JsonDecoder[CurrencySymbol] =
      DeriveJsonDecoder.gen[CurrencySymbol]
  }

  object Currency {
    import CurrencySymbol.{decoder, encoder}

    /**
     * Implicit JSON encoder for Currency.
     */
    implicit val encoder: JsonEncoder[Currency] =
      DeriveJsonEncoder.gen[Currency]

    /**
     * Implicit JSON decoder for Currency.
     */
    implicit val decoder: JsonDecoder[Currency] =
      DeriveJsonDecoder.gen[Currency]
  }
}
