package dev.cmartin.learn

import zio.*
import zio.http.*

import java.util.UUID

object ZIOHttpServer extends ZIOAppDefault {

  private def findOrder(id: UUID): UIO[Response] =
    for {
      _ <- ZIO.logInfo(s"requested order id: $id")
    } yield Response.text(s"Requested Order ID: $id")

  private val routes =
    Routes(
      Method.GET / Root                  -> handler(Response.text("Greetings at your service")),
      Method.GET / "greet"               -> handler { (req: Request) =>
        val name = req.queryParamToOrElse("name", "World")
        Response.text(s"Hello $name!")
      },
      Method.GET / "orders" / uuid("id") ->
        handler { (id: UUID, req: Request) =>
          findOrder(id)
        }
    ) @@ HandlerAspect.debug

  def run: Task[Nothing] =
    Server.serve(routes).provide(Server.default)
}

