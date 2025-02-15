package com.cmartin.utils.poc

import zio.UIO

import java.time.LocalDate
import java.util.UUID

/** Common assets or services offered to retail clients in banking.
  */
object BankingGlobalPositionPoc {

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

  private def getCardResume(clientId: UUID): UIO[CardResume] = ???

  private def getSavingsAccountResume(clientId: UUID): UIO[SavingsAccountResume] = ???

  private def getLoanResume(clientId: UUID): UIO[LoanResume] = ???

  // get global position calling functions in parallel
  def getGlobalPosition(clientId: UUID): UIO[GlobalPosition] =
    getCardResume(clientId)
      .zipPar(getSavingsAccountResume(clientId))
      .zipPar(getLoanResume(clientId))
      .map { case (cardResume, savingsAccountResume, loanResume) =>
        GlobalPosition(cardResume, savingsAccountResume, loanResume)
      }

}
