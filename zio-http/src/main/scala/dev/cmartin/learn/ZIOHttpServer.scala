package dev.cmartin.learn

import dev.cmartin.learn.DomainData.currencyMap
import dev.cmartin.learn.DomainModel.{Currency, CurrencySymbol}
import dev.cmartin.learn.JsonCodecs.Currency.*
import zio.*
import zio.http.*
import zio.json.EncoderOps

import java.util.UUID

object ZIOHttpServer
    extends ZIOAppDefault {

  private def symbolToEnum(symbol: String): Option[CurrencySymbol] =
    CurrencySymbol.values.find(_.toString == symbol)

  private def findCurrency(symbol: String): UIO[Response] =
    ZIO.succeed(
      symbolToEnum(symbol)
        .fold(Response.status(Status.BadRequest))(a =>
          currencyMap.get(a)
            .fold(Response.status(Status.NotFound))(a =>
              Response.json(a.toJson)
            )
        )
    )

  private def findOrder(id: UUID): UIO[Response] =
    for {
      _ <- ZIO.logInfo(s"requested order id: $id")
    } yield Response.text(s"Requested Order ID: $id")

  private def REMOVE_findCurrency(symbol: String): UIO[Response] =
    for {
      _        <- ZIO.logInfo(s"requested currency symbol: $symbol")
      currency <- ZIO.succeed(Currency(UUID.randomUUID(), "Euro", CurrencySymbol.EUR))
    } yield Response.json(currency.toJson)

  private val routes =
    Routes(
      Method.GET / Root                            -> handler(Response.text("Greetings at your service")),
      Method.GET / "greet"                         -> handler { (req: Request) =>
        val name = req.queryParamToOrElse("name", "World")
        Response.text(s"Hello $name!")
      },
      Method.GET / "orders" / uuid("id")           ->
        handler { (id: UUID, req: Request) =>
          findOrder(id)
        },
      Method.GET / "currencies" / string("symbol") ->
        handler { (symbol: String, req: Request) =>
          findCurrency(symbol)
        }
    ) @@ HandlerAspect.debug

  def run: Task[Nothing] =
    Server.serve(routes).provide(Server.default)
}
