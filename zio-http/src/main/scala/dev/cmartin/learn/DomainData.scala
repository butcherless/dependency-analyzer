package dev.cmartin.learn

import dev.cmartin.learn.DomainModel.{Currency, CurrencySymbol}

import java.util.UUID

object DomainData {

  // UUIDs for different currencies
  private val euroId: UUID   = UUID.fromString("513b7611-6645-499c-974c-edd2adb1d6d8")
  private val dollarId: UUID = UUID.fromString("c83d19ed-671b-40ea-b08b-37b80e6d523f")
  private val yenId: UUID    = UUID.fromString("3e90de53-bc7c-4f90-9ac1-85b9c59c0396")
  private val poundId: UUID  = UUID.fromString("08f06820-5b2b-47e2-af22-ab12055c8b78")
  private val francId: UUID  = UUID.fromString("2a704f21-3083-49ce-b29c-063ef36faa6e")

  // Currency instances with their respective UUIDs, names, and symbols
  private val euro: Currency   = Currency(euroId, "Euro", CurrencySymbol.EUR)
  private val dollar: Currency = Currency(dollarId, "US dollar", CurrencySymbol.USD)
  private val yen: Currency    = Currency(yenId, "Japanese yen", CurrencySymbol.JPY)
  private val pound: Currency  = Currency(poundId, "Pound sterling", CurrencySymbol.GBP)
  private val franc: Currency  = Currency(francId, "Swiss franc", CurrencySymbol.CHF)

  // List of all currency instances
  private val currencies: Seq[Currency] = List(euro, dollar, yen, pound, franc)

  // Map of currency symbols to their respective currency instances
  val currencyMap: Map[CurrencySymbol, Currency] = currencies.map(a => a.symbol -> a).toMap
}
