package dev.cmartin.learn

import java.util.UUID
import dev.cmartin.learn.DomainModel.Currency

object DomainData {
  val euroId   = UUID.fromString("513b7611-6645-499c-974c-edd2adb1d6d8")
  val dollarId = UUID.fromString("c83d19ed-671b-40ea-b08b-37b80e6d523f")
  val yenId    = UUID.fromString("3e90de53-bc7c-4f90-9ac1-85b9c59c0396")
  val poundId  = UUID.fromString("08f06820-5b2b-47e2-af22-ab12055c8b78")
  val francId  = UUID.fromString("2a704f21-3083-49ce-b29c-063ef36faa6e")

  val euro   = Currency(euroId, "Euro", "EUR")
  val dollar = Currency(euroId, "US dollar", "USD")
  val yen    = Currency(euroId, "Japanese yen", "JPY")
  val pound  = Currency(euroId, "Pound sterling", "GBP")
  val franc  = Currency(euroId, "Swiss franc", "CHF")

  val currencies = List(euro, dollar, yen, pound, franc)

  val currencyMap = currencies.map(a => a.symbol -> a).toMap
}
