package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import co.elastic.apm.attach.ElasticApmAttacher
import com.example.api.{UserRegistry, UserRegistryClient, UserRoutes}

import scala.util.Failure
import scala.util.Success

object QuickstartApp {
  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
    import system.executionContext

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
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
    ElasticApmAttacher.attach()

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
      val timeout = Timeout.create(
        context.system.settings.config.getDuration("my-app.routes.ask-timeout")
      )
      val userRegistryClient =
        new UserRegistryClient(userRegistryActor)(context.system, timeout)
      context.watch(userRegistryActor)

      val routes = new UserRoutes(userRegistryClient)(context.system)
      startHttpServer(routes.userRoutes, context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
