package com.cmartin.utils.poc

import zio.{IO, UIO, ZIO}
import zio.Schedule

object MultiWebCientPoc {

  sealed trait ClientRequest
  case class RequestOne(s: String)         extends ClientRequest
  case class RequestTwo(s: String, n: Int) extends ClientRequest

  sealed trait ClientResponse
  case class ResponseOne(n: Long, b: Boolean)   extends ClientResponse
  case class ResponseTwo(s: String, b: Boolean) extends ClientResponse

  sealed trait DomainError
  case class ErrorOne()   extends DomainError
  case class ErrorTwo()   extends DomainError
  case class ErrorThree() extends DomainError

  trait WebClientService[Req, Res] {
    def execute(req: Req): IO[DomainError, Res]
  }

  case class OneWebClientService() extends WebClientService[RequestOne, ResponseOne] {
    override def execute(req: RequestOne): IO[DomainError, ResponseOne] = ???
  }

  case class TwoWebClientService() extends WebClientService[RequestTwo, ResponseTwo] {
    override def execute(req: RequestTwo): IO[DomainError, ResponseTwo] = ???
  }

  val clientOne: OneWebClientService = ???
  val clientTwo: TwoWebClientService = ???

  val requests: List[ClientRequest] = ???
  val fiberNumber: Int              = ???

  /* number of times, status code cases, exponential, etc. */
  val oneRetryPolicy: Schedule[Any, DomainError, ResponseOne] = ???
  val twoRetryPolicy: Schedule[Any, DomainError, ResponseTwo] = ???

  /* make requests, grouped failures & successes */
  val responses: UIO[(Iterable[DomainError], Iterable[ClientResponse])] =
    ZIO.partitionPar(requests) {
      case req: RequestOne => clientOne.execute(req).retry(oneRetryPolicy)
      case req: RequestTwo => clientTwo.execute(req).retry(twoRetryPolicy)
    }.withParallelism(fiberNumber)

  /* process success responses */
  val successResults: UIO[String] =
    responses.map {
      case (successList, _) => successList
    }.map {
      case res: ResponseOne => s"${res.n}:${res.b}"
      case res: ResponseTwo => s"${res.s}:${res.b}"
    }

  /* process failed responses */
  val failedResults: UIO[String] =
    responses.map {
      case (_, errorList) => errorList
    }.map {
      case res: ErrorOne   => s"$res"
      case res: ErrorTwo   => s"$res"
      case res: ErrorThree => s"$res"
    }
}
