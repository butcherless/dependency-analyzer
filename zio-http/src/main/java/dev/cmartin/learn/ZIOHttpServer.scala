package dev.cmartin.learn

import zio.*
import zio.http.*

object ZIOHttpServer extends ZIOAppDefault {
  private val routes =
    Routes(
      Method.GET / Root    -> handler(Response.text("Greetings at your service")),
      Method.GET / "greet" -> handler { (req: Request) =>
        val name = req.queryParamToOrElse("name", "World")
        Response.text(s"Hello $name!")
      }
    )

  def run: Task[Nothing] =
    Server.serve(routes).provide(Server.default)
}
