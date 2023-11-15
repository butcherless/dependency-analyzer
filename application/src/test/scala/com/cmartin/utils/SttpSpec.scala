package com.cmartin.utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.capabilities.zio.ZioStreams
import sttp.client4.WebSocketStreamBackend
import sttp.client4._
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.StatusCode
import zio.Task
import zio._

class SttpSpec
    extends AnyFlatSpec
    with Matchers {

  import SttpSpec._

  // TODO Sttp/Zio-2 implicit val backend = Runtime.default.unsafeRun(AsyncHttpClientZioBackend())

  behavior of "Sttp client"

  "Raw interpolator" should "build an encoded string" in {
    val group    = "dev.zio"
    val artifact = "zio_2.13"

    val filter = s"q=g:$group+AND+a:$artifact+AND+p:jar&rows=1&wt=json"
    val uri    = raw"https://search.maven.org/solrsearch/select?$filter"

    info(uri)
  }

  "sttp client" should "make a HEAD request" in {
    val program = HttpClientZioBackend().flatMap { backend =>
      makeHeadRequest(backend)
    }

    val res = TestUtils.run(program.either)

    res shouldBe Right(StatusCode.Ok)
  }

  ignore should "make a GET request" in {
    val program   = HttpClientZioBackend().flatMap { backend =>
      makeGetRequest(backend)
    }
    val resEither = TestUtils.run(program.either)

    resEither.isRight shouldBe true
    val body = resEither.getOrElse("")
    body.nonEmpty shouldBe true
    info(s"body: $body")
  }

  // TODO integration test
  ignore should "make a GET request sequence" in {
    val urls = Seq(
      "https://apache.org/",
      "https://sttp.softwaremill.com/",
      "https://httpbin.org/get",
      "https://zio.dev/",
      "https://github.com/",
      "https://gitlab.com/",
      "https://www.google.com/",
      "https://www.mutua.es/",
      "https://typelevel.org/cats/",
      "https://www.lightbend.com/",
      "https://www.scala-lang.org/",
      "https://www.scala-sbt.org/",
      "https://www.oracle.com/",
      "https://www.postgresql.org/",
      "https://www.jetbrains.com/",
      "https://www.apple.com/es/",
      "https://www.elastic.co/",
      "https://prometheus.io/",
      "https://www.youtube.com/",
      "https://kubernetes.io/",
      "https://degoes.net/",
      "https://elpais.com/"
    )

    val program = HttpClientZioBackend().flatMap { backend =>
      makeGetRequests(backend, urls)
    }

    val res = TestUtils.run(program.either)

    info(s"result: $res")
    res.isRight shouldBe true
    res.map(as =>
      as.forall(_ == StatusCode.Ok) shouldBe true
    )
  }

  def makeGetRequests(backend: HttpBackend, urls: Seq[String]) = {

    def makeGetRequest(url: String) =
      for {
        _        <- ZIO.log(s"trying get to URL: $url")
        response <- getHeadRequest(url).send(backend).mapError(e => s"${e.getMessage()}")
        code     <- manageStatusCode(response.code)
      } yield code

    ZIO.foreachPar(urls)(makeGetRequest).withParallelism(4)
  }

  def makeHeadRequest(backend: HttpBackend): IO[String, StatusCode] =
    for {
      response <- getHeadRequest("https://apache.org/")
                    .send(backend).mapError(e => s"${e.getMessage()}")
      code     <- manageStatusCode(response.code)
    } yield code

  def makeGetRequest(backend: HttpBackend): IO[String, String] =
    for {
      response <- buildGetRequest("http://localhost:8180/realms/pelayo-desa/.well-known/openid-configuration")
                    .send(backend).mapError(e => s"${e.getMessage()}")
      result   <- ZIO.fromEither(response.body)
    } yield result

  object SttpSpec {
    type HttpBackend = WebSocketStreamBackend[Task, ZioStreams]

    def getHeadRequest(url: String) =
      basicRequest
        .head(uri"$url")
        .response(asString)

    def manageStatusCode(code: StatusCode) =
      code match {
        case x if x.isSuccess => ZIO.succeed(code)
        case _                => ZIO.fail(s"client request or server error with code: $code")
      }

    def buildGetRequest(url: String): Request[Either[String, String]] =
      basicRequest
        .get(uri"$url")
        .response(asString)

  }

  /*
  ignore should "make a post request" in {
    val postRequest = basicRequest
      .post(uri"http://httpbin.org/post")
      .body("dummy post body")

    info(postRequest.toCurl)

    val postResponse: Task[Response[Either[String, String]]] =
      postRequest.send()

    val bodyResult: URIO[Any, Either[Throwable, Response[Either[String, String]]]] =
      postResponse.either

    val result: Either[Throwable, Response[Either[String, String]]] =
      runtime.unsafeRun(bodyResult)

    result match {
      case Right(value) =>
        value.body match {
          case Right(value) =>
            info(value)
            value.contains("dummy post body") shouldBe true

          case Left(value) => fail("expected successful result")
        }
      case Left(value) => fail(s"expected successful result: $value")
    }
  }

  ignore should "make a GET request" in {
    val group = "dev.zio"
    val artifact = "zio_2.13"

    val filter = s"q=g:$group+AND+a:$artifact+AND+p:jar&rows=1&wt=json"
    val rawUri = raw"https://search.maven.org/solrsearch/select?$filter"

    val getRequest = basicRequest
      .get(uri"$rawUri")

    info(getRequest.toCurl)

    val getResponse = getRequest.send()
    val bodyResult = getResponse.either
    val result: Either[Throwable, Response[Either[String, String]]] =
      runtime.unsafeRun(bodyResult)

    result match {
      case Right(value) =>
        value.body match {
          case Right(value) =>
            info(value)
            value.contains("zio") shouldBe true

          case Left(_) => fail("expected successful result")
        }
      case Left(value) => fail(s"expected successful result: $value")
    }
  }
   */
}
