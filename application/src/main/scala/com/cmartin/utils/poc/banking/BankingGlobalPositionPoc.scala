package com.cmartin.utils.poc.banking

import com.cmartin.utils.poc.banking.Model.*
import zio.*

import java.util.UUID

/** Common assets or services offered to retail clients in banking.
  */
object BankingGlobalPositionPoc
    extends ZIOAppDefault {

  private def generateDelay(): UIO[zio.Duration] =
    for {
      delay <- Random.nextIntBetween(Data.MIN_DELAY_MILLIS, Data.MAX_DELAY_MILLIS)
      _     <- ZIO.logInfo(s"generated delay: $delay millis")
    } yield delay.milliseconds

  private def getCardResume(clientId: UUID): UIO[CardResume] =
    for {
      debitCard <- Data.createDebitCardOne
      delay     <- generateDelay()
      _         <- ZIO.sleep(delay)
      _         <- ZIO.logInfo("retrieved debit card resume for client: $clientId")
    } yield CardResume(List(debitCard), debitCard.balance)

  private def getSavingsAccountResume(clientId: UUID): UIO[SavingsAccountResume] =
    for {
      savingsAccount <- Data.createSavingAccountOne
      delay          <- generateDelay()
      _              <- ZIO.sleep(delay)
      _              <- ZIO.logInfo("retrieved savings account resume for client: $clientId")
    } yield SavingsAccountResume(List(savingsAccount), savingsAccount.balance)

  private def getLoanResume(clientId: UUID): UIO[LoanResume] =
    for {
      loan  <- Data.createLoanOne
      delay <- generateDelay()
      _     <- ZIO.sleep(delay)
      _     <- ZIO.logInfo("retrieved loan resume for client: $clientId")
    } yield LoanResume(List(loan), loan.balance)

  private def getFundResume(clientId: UUID): UIO[FundResume] =
    for {
      fund  <- Data.createFundOne
      delay <- generateDelay()
      _     <- ZIO.sleep(delay)
      _     <- ZIO.logInfo("retrieved fund resume for client: $clientId")
    } yield FundResume(List(fund), fund.balance)

  // get global position calling functions in parallel
  // <&> is an alias for '.zipPar' operator
  private def getGlobalPosition(clientId: UUID): UIO[GlobalPosition] =
    for {
      _                <- ZIO.logInfo(s"getting global position for client: $clientId")
      (cs, as, ls, fs) <- getCardResume(clientId)
                            <&> getSavingsAccountResume(clientId)
                            <&> getLoanResume(clientId)
                            <&> getFundResume(clientId)
      _                <- ZIO.logInfo(s"retrieved global position for client: $clientId")
    } yield GlobalPosition(cs, as, ls, fs)

  /*
   * program
   */
  val program: UIO[Unit] =
    for {
      _              <- ZIO.logInfo("zio banking demo app")
      globalPosition <- getGlobalPosition(Data.CLIENT_ONE_ID)
    } yield ()

  // main function, needs exit = 0 [OK] or exit > 0 [ERROR]
  // Here the interpreter runs the program and perform side effects
  override def run: UIO[Unit] =
    program

}
