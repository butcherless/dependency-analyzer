package com.cmartin.utils.poc

import com.cmartin.utils.poc.SttpWebClientPoc.ClientError.{PayloadError, ProtocolError, UnknownError}
import com.cmartin.utils.poc.SttpWebClientPoc.Model.SlideShowResponse
import sttp.capabilities.zio.ZioStreams
import sttp.client4._
import sttp.client4.ziojson._
import sttp.model.{MediaType, StatusCode}
import zio.json.{DeriveJsonDecoder, JsonDecoder}
import zio.{IO, Task, ZIO}

object SttpWebClientPoc {

  type HttpBackend = WebSocketStreamBackend[Task, ZioStreams]

  sealed trait ClientError extends Product with Serializable

  object ClientError {
    final case class ProtocolError(message: String) extends ClientError
    final case class PayloadError(message: String)  extends ClientError
    final case class UnknownError(message: String)  extends ClientError
  }

  object Model {
    final case class Slide(
        title: String
    )
    object Slide {
      implicit val decoder: JsonDecoder[Slide] = DeriveJsonDecoder.gen[Slide]
    }
    final case class Slideshow(
        author: String,
        date: String,
        slides: List[Slide],
        title: String
    )

    object Slideshow {
      implicit val decoder: JsonDecoder[Slideshow] = DeriveJsonDecoder.gen[Slideshow]
    }
    final case class SlideShowResponse(
        slideshow: Slideshow
    )

    object SlideShowResponse {
      implicit val decoder: JsonDecoder[SlideShowResponse] = DeriveJsonDecoder.gen[SlideShowResponse]
    }

  }

  def makeGetJsonRequest(backend: HttpBackend): IO[ClientError, SlideShowResponse] =
    for {
      resEither <- buildGetJsonRequest("https://httpbin.org/json")
                     .send(backend).mapError(e => UnknownError(e.getMessage))
      response  <- getResponseOkOrError(resEither)
      body      <- getResponseBodyOrError(response)
    } yield body

  def buildGetJsonRequest(url: String) =
    basicRequest
      .contentType(MediaType.ApplicationJson)
      .get(uri"$url")
      .response(asJson[SlideShowResponse])

  def getResponseOkOrError(response: Response[Either[ResponseException[String, String], SlideShowResponse]]) =
    if (response.code == StatusCode.Ok) ZIO.succeed(response.body)
    else ZIO.fail(ProtocolError(s"response error: ${response.code}"))

  def getResponseBodyOrError(bodyEither: Either[ResponseException[String, String], SlideShowResponse]) =
    bodyEither
      .fold(
        e => ZIO.fail(PayloadError(s"payload error: ${e.getMessage}")),
        a => ZIO.succeed(a)
      )
}
