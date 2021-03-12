package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

import scala.concurrent.Future

//#main-class
object QuickstartApp {
  //#start-http-server
  private def startHttpServer(
      routes: Route,
      userDatabase: UserDatabase
  )(implicit
      system: ActorSystem[_]
  ): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = for {
      _ <- userDatabase.createTables
      binding <- Http().newServerAt("localhost", 8080).bind(routes)
    } yield binding

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userDatabase = new UserDatabase()(context.system)
      val routes = new UserRoutes(userDatabase)(context.system)
      startHttpServer(routes.userRoutes, userDatabase)(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
