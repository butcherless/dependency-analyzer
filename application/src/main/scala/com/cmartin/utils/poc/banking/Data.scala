package com.cmartin.utils.poc.banking

import com.cmartin.utils.poc.banking.Model.*
import zio.{UIO, ZIO}

import java.time.LocalDate
import java.util.UUID

object Data {
  val MIN_DELAY_MILLIS = 500
  val MAX_DELAY_MILLIS = 4000

  val CLIENT_ONE_ID: UUID = UUID.fromString("1722f6e5-64f1-404f-89f8-b1cec7fcb34a")

  private val EXPIRE_ONE_DATE            = LocalDate.of(2025, 12, 31)
  private val DEBIT_CARD_ONE_ID          = UUID.fromString("177b5021-b8aa-476a-946a-a2a40763fa25")
  private val DEBIT_CARD_ONE_CODE        = "1000200030004000"
  private val DEBIT_CARD_ONE_EXPIRE_DATE = EXPIRE_ONE_DATE
  private val DEBIT_CARD_ONE_BALANCE     = BigDecimal(125)

  private val SAVINGS_ACCOUNT_ONE_ID      = UUID.fromString("b5d94f17-e677-4f67-b2b1-81b23fcd0995")
  private val SAVINGS_ACCOUNT_ONE_CODE    = "ES1020003000405000000000"
  private val SAVINGS_ACCOUNT_ONE_BALANCE = BigDecimal(250)

  private val LOAN_ONE_ID      = UUID.fromString("235652f4-c92e-4bfd-8de6-076ce9b24503")
  private val LOAN_ONE_CODE    = "LOAN-1000"
  private val LOAN_ONE_BALANCE = BigDecimal(5001)

  private val FUND_ONE_ID           = UUID.fromString("10c1ac93-0498-46c3-bc96-63d216c9e554")
  private val FUND_ONE_ASSET_TYPE   = AssetType.Bond
  private val FUND_ONE_RISK_PROFILE = RiskProfile.Moderate
  private val FUND_ONE_SHARE_COUNT  = 1500
  private val FUND_ONE_BALANCE      = BigDecimal(15000)

  private def createDebitCard(
      id: UUID,
      code: String,
      expireDate: LocalDate,
      balance: BigDecimal
  ): DebitCard =
    DebitCard(
      id = id,
      code = code,
      expireDate = expireDate,
      balance = balance
    )

  private def createSavingAccount(
      id: UUID,
      code: String,
      balance: BigDecimal
  ): SavingsAccount =
    SavingsAccount(
      id = id,
      code = code,
      balance = balance
    )

  private def createLoan(
      id: UUID,
      code: String,
      balance: BigDecimal
  ): Loan =
    Loan(
      id = id,
      code = code,
      balance = balance
    )

  private def createFund(
      id: UUID,
      assetType: AssetType,
      riskProfile: RiskProfile,
      shareCount: Int,
      balance: BigDecimal
  ): Fund =
    Fund(
      id = id,
      assetType = assetType,
      riskProfile = riskProfile,
      shareCount = shareCount,
      balance = balance
    )

  def createDebitCardOne: UIO[DebitCard] =
    ZIO.succeed(
      createDebitCard(
        DEBIT_CARD_ONE_ID,
        DEBIT_CARD_ONE_CODE,
        DEBIT_CARD_ONE_EXPIRE_DATE,
        DEBIT_CARD_ONE_BALANCE
      )
    )

  def createSavingAccountOne: UIO[SavingsAccount] =
    ZIO.succeed(
      createSavingAccount(
        SAVINGS_ACCOUNT_ONE_ID,
        SAVINGS_ACCOUNT_ONE_CODE,
        SAVINGS_ACCOUNT_ONE_BALANCE
      )
    )

  def createLoanOne: UIO[Loan] =
    ZIO.succeed(
      createLoan(
        LOAN_ONE_ID,
        LOAN_ONE_CODE,
        LOAN_ONE_BALANCE
      )
    )

  def createFundOne: UIO[Fund] =
    ZIO.succeed(
      createFund(
        FUND_ONE_ID,
        FUND_ONE_ASSET_TYPE,
        FUND_ONE_RISK_PROFILE,
        FUND_ONE_SHARE_COUNT,
        FUND_ONE_BALANCE
      )
    )
}
