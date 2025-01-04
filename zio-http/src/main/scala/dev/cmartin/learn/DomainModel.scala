package dev.cmartin.learn

import java.util.UUID

object DomainModel {
  enum CurrencySymbol(val description: String):
    case EUR extends CurrencySymbol("Euro")
    case USD extends CurrencySymbol("US Dollar")
    case JPY extends CurrencySymbol("Japanese Yen")
    case GBP extends CurrencySymbol("Pound Sterling")
    case CHF extends CurrencySymbol("Swiss Franc")
    case BRL extends CurrencySymbol("Brazilian Real")

  case class Currency(id: UUID, name: String, symbol: CurrencySymbol)
  case class Amount(currency: Currency, value: Double)
  case class Campaign(id: UUID, year: Int, term: String)
  case class Customer(id: UUID, responsible: String)
  case class Product(id: UUID, name: String, costPrice: Double)
  case class OrderLine(id: UUID, campaign: Campaign, cost: Double, lineNumber: Int)
  case class Order(id: UUID, customer: Customer, product: Product, line: OrderLine)
  case class Negotiation(id: UUID, customerId: UUID, campaign: Campaign, amount: Amount)
}
