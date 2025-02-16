package com.cmartin.utils.poc.banking

import java.time.LocalDate
import java.util.UUID

object Model {
  case class SavingsAccount(
      id: UUID,
      code: String,
      balance: BigDecimal
  )

  case class SavingsAccountResume(
      accounts: List[SavingsAccount],
      totalBalance: BigDecimal
  )

  sealed trait Card {
    val id: UUID
    val code: String
    val expireDate: LocalDate
    val balance: BigDecimal
  }

  case class DebitCard(
      id: UUID,
      code: String,
      expireDate: LocalDate,
      balance: BigDecimal
  ) extends Card

  case class CreditCard(
      id: UUID,
      code: String,
      expireDate: LocalDate,
      nextChargeDate: LocalDate,
      balance: BigDecimal
  ) extends Card

  case class CardResume(
      cards: List[Card],
      totalBalance: BigDecimal
  )

  case class Loan(
      id: UUID,
      code: String,
      balance: BigDecimal
  )

  case class LoanResume(
      loans: List[Loan],
      totalBalance: BigDecimal
  )

  case class GlobalPosition(
      cardResume: CardResume,
      savingsAccountResume: SavingsAccountResume,
      loanResume: LoanResume
  )
}
