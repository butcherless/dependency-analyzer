package com.cmartin.utils.poc.banking

import java.time.LocalDate
import java.util.UUID

object Model {

  /** Savings account
    * @param id
    *   account identifier
    * @param code
    *   account code
    * @param balance
    *   account balance
    */
  case class SavingsAccount(
      id: UUID,
      code: String,
      balance: BigDecimal
  )

  /** Summary of savings accounts
    *
    * @param accounts
    *   list of savings accounts
    * @param totalBalance
    *   total balance of all savings accounts
    */
  case class SavingsAccountResume(
      accounts: List[SavingsAccount],
      totalBalance: BigDecimal
  )

  /** Trait representing a generic card
    */
  sealed trait Card {
    val id: UUID
    val code: String
    val expireDate: LocalDate
    val balance: BigDecimal
  }

  /** Debit card
    * @param id
    *   card identifier
    * @param code
    *   card code
    * @param expireDate
    *   card expiration date
    * @param balance
    *   card balance
    */
  case class DebitCard(
      id: UUID,
      code: String,
      expireDate: LocalDate,
      balance: BigDecimal
  ) extends Card

  /** Credit card
    * @param id
    *   card identifier
    * @param code
    *   card code
    * @param expireDate
    *   card expiration date
    * @param nextChargeDate
    *   date of the next charge
    * @param balance
    *   card balance
    */
  case class CreditCard(
      id: UUID,
      code: String,
      expireDate: LocalDate,
      nextChargeDate: LocalDate,
      balance: BigDecimal
  ) extends Card

  /** Summary of cards
    *
    * @param cards
    *   list of cards
    * @param totalBalance
    *   total balance of all cards
    */
  case class CardResume(
      cards: List[Card],
      totalBalance: BigDecimal
  )

  /** Loan
    *
    * @param id
    *   loan identifier
    * @param code
    *   loan code
    * @param balance
    *   loan balance
    */
  case class Loan(
      id: UUID,
      code: String,
      balance: BigDecimal
  )

  /** Summary of loans
    *
    * @param loans
    *   list of loans
    * @param totalBalance
    *   total balance of all loans
    */
  case class LoanResume(
      loans: List[Loan],
      totalBalance: BigDecimal
  )

  /** Global financial position
    *
    * @param cardResume
    *   summary of cards
    * @param savingsAccountResume
    *   summary of savings accounts
    * @param loanResume
    *   summary of loans
    * @param fundResume
    *   summary of funds
    */
  case class GlobalPosition(
      cardResume: CardResume,
      savingsAccountResume: SavingsAccountResume,
      loanResume: LoanResume,
      fundResume: FundResume
  )

  enum AssetType:
    case Stock
    case Bond
    case ReasEstate

  enum RiskProfile:
    case Conservative
    case Moderate
    case Aggressive

  /** Fund
    * @param id
    *   fund identifier
    * @param assetType
    *   type of asset
    * @param riskProfile
    *   risk profile of the fund
    * @param shareCount
    *   number of shares
    * @param balance
    *   fund balance
    */
  case class Fund(
      id: UUID,
      assetType: AssetType,
      riskProfile: RiskProfile,
      shareCount: Int,
      balance: BigDecimal
  )

  /** Summary of funds
    *
    * @param funds
    *   list of funds
    * @param totalBalance
    *   total balance of all funds
    */
  case class FundResume(
      funds: List[Fund],
      totalBalance: BigDecimal
  )
}
