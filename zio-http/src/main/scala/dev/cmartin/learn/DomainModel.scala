package dev.cmartin.learn

import java.util.UUID

object DomainModel {
  enum CurrencySymbol:
    case EUR, USD, JPY, GBP, CHF, BRL

  case class Currency(id: UUID, name: String, symbol: CurrencySymbol)
  case class Amount(currency: Currency, value: Double)
  case class Campaign(id: UUID, year: Int, term: String)
  case class Customer(id: UUID, responsible: String)
  case class Product(id: UUID, name: String, costPrice: Double)
  case class OrderLine(id: UUID, campaign: Campaign, cost: Double, lineNumber: Int)
  case class Order(id: UUID, customer: Customer, product: Product, line: OrderLine)
  case class Negotiation(id: UUID, customerId: UUID, campaign: Campaign, ammount: Amount)
}
