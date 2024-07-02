package com.cmartin.utils.poc

import zio.{IO, Schedule, UIO, ZIO}

/** Defines a generic web client for performing parallel operations and
  * processing success and failure responses.
  */
object MultiWebCientPoc {

  /** Client request ADT
    */
  sealed trait ClientRequest

  case class GreenRequest(s: String) extends ClientRequest

  case class BlueRequest(s: String, n: Int) extends ClientRequest

  /** Client response ADT
    */
  sealed trait ClientResponse

  case class GreenResponse(n: Long, b: Boolean) extends ClientResponse

  case class BlueResponse(s: String, b: Boolean) extends ClientResponse

  /** Domain error ADT
    */
  sealed trait DomainError

  case class ErrorOne() extends DomainError

  case class ErrorTwo() extends DomainError

  case class ErrorThree() extends DomainError

  /** Generic web client
    */
  trait WebClientService[ClientRequest, ClientResponse] {
    def execute(req: ClientRequest): IO[DomainError, ClientResponse]
  }

  /** Specific green web client
    */
  case class GreenWebClientService()
      extends WebClientService[GreenRequest, GreenResponse] {
    override def execute(req: GreenRequest): IO[DomainError, GreenResponse] = ???
  }

  /** Specific blue web client
    */
  case class BlueWebClientService()
      extends WebClientService[BlueRequest, BlueResponse] {
    override def execute(req: BlueRequest): IO[DomainError, BlueResponse] = ???
  }

  val greenClient: GreenWebClientService = ???
  val blueClient: BlueWebClientService   = ???

  /* number of times, status code cases, exponential, etc. */
  val greenRetryPolicy: Schedule[Any, DomainError, GreenResponse] = ???
  val blueRetryPolicy: Schedule[Any, DomainError, BlueResponse]   = ???

  /** process request and produces response
    *
    * @param request
    *   input data
    * @return
    *   output data
    */
  def execute(request: ClientRequest): IO[DomainError, ClientResponse] =
    request match
      case req: GreenRequest =>
        greenClient.execute(req)
          .retry(greenRetryPolicy)
      case req: BlueRequest  =>
        blueClient.execute(req)
          .retry(blueRetryPolicy)

  def processSuccess(response: ClientResponse): String =
    response match {
      case res: GreenResponse => s"${res.n}:${res.b}"
      case res: BlueResponse  => s"${res.s}:${res.b}"
    }

  def processFail(error: DomainError): String =
    error match {
      case res: ErrorOne   => s"$res"
      case res: ErrorTwo   => s"$res"
      case res: ErrorThree => s"$res"
    }

  /* Requests */
  val requests: List[ClientRequest] = ???
  val fiberNumber: Int              = ???

  /* make requests, grouped failures & successes */
  val responses: UIO[(Iterable[DomainError], Iterable[ClientResponse])] =
    ZIO.partitionPar(requests)(execute)
      .withParallelism(fiberNumber)

  /* process success responses and fold them */
  val successResults: UIO[String] =
    responses.map {
      case (_, successList) => successList.map(processSuccess).mkString("[", ",", "]")
    }

  /* process failed responses */
  val failedResults: UIO[String] =
    responses.map {
      case (errorList, _) => errorList.map(processFail).mkString("[", ",", "]")
    }
}
