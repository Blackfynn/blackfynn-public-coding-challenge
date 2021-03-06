package com.blackfynn

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.Failure
import scala.util.Success

import scala.concurrent.Future

object QuickstartApp {

  private def startHttpServer(
      routes: Route,
      participantDatabase: ParticipantDatabase
  )(implicit
      system: ActorSystem[_]
  ): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    // Bootstrap database and server
    val futureBinding = for {
      _ <- participantDatabase.createTables
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

  def main(args: Array[String]): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val participantDatabase = new ParticipantDatabase()(context.system)
      val routes = new ParticipantRoutes(participantDatabase)(context.system)
      startHttpServer(routes.participantRoutes, participantDatabase)(
        context.system
      )

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "BlackfynnChallengeApp")
  }
}
