package com.example.api

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.{Route, RouteResult}
import co.elastic.apm.api.{ElasticApm, Scope, Transaction}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class UserRoutes(userRegistry: UserRegistryClient)(implicit val system: ActorSystem[_]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private def mapRouteResultWithTransaction(transaction: Transaction, scope: Scope): RouteResult => RouteResult = { result =>
    result match {
      case Complete(httpResponse: HttpResponse) =>
        val statusCode = httpResponse.status.intValue
        val statusRange = (statusCode / 100).toString + "xx"
        transaction.setLabel("status_code", statusCode.toString)
        transaction.setResult(s"HTTP $statusRange")
      case Rejected(_) =>
        transaction.setLabel("status_code", "500")
        transaction.setResult("HTTP 5xx")
    }
    println("transaction-id: " +  ElasticApm.currentTransaction().getId + " Ending transaction " + transaction.getId  + " thread=" + Thread.currentThread().getId)

    scope.close()
    transaction.end()
    result
  }

  def withTraceContext(route: Route): Route = { ctx =>
    val transaction = ElasticApm.startTransaction()
    var scope: Scope = null
    try {
      scope = transaction.activate()
    } catch {
      case NonFatal(_) =>
        if (scope != null) {
          scope.close()
        }
    }
    println("transaction-id: " +  ElasticApm.currentTransaction().getId + "Starting transaction " + transaction.getId + " thread=" + Thread.currentThread().getId)
    transaction.setType(Transaction.TYPE_REQUEST)
    transaction.setName(ctx.request.method.value + " " + ctx.request.uri.path.toString)
    transaction.setLabel("path", ctx.request.uri.path.toString)
    transaction.setLabel("request_method", ctx.request.method.value)

    mapRouteResult(mapRouteResultWithTransaction(transaction, scope))(route)(ctx)
  }

  implicit val executionContext: ExecutionContext = system.executionContext

  def waitForIt(): Future[Unit] = {
    println("transaction-id: " +  ElasticApm.currentTransaction().getId + " waitForIt before " + " thread=" + Thread.currentThread().getId)
    Future {
      println("transaction-id: " +  ElasticApm.currentTransaction().getId + " waitForIt start" + " thread=" + Thread.currentThread().getId)
      val span1 = ElasticApm.currentSpan().startSpan("custom", "Thread", "sleep")
      span1.setName("Thread.sleep")
      Thread.sleep(500)
      span1.end()

      println("transaction-id: " +  ElasticApm.currentTransaction().getId + " waitForIt end" + " thread=" + Thread.currentThread().getId)
    }
  }

  val userRoutes: Route = {
    withTraceContext {
      pathPrefix("users") {
        concat(
          pathEnd {
            concat(
              get {
                println("transaction-id: " +  ElasticApm.currentTransaction().getId + " GET users route start" + " thread=" + Thread.currentThread().getId)
                onSuccess(waitForIt().flatMap(_ => userRegistry.getUsers())) { users =>
                  println("transaction-id: " +  ElasticApm.currentTransaction().getId + " GET users route done" + " thread=" + Thread.currentThread().getId)
                  complete((StatusCodes.OK, users))
                }
              },
              post {
                entity(as[User]) { user =>
                  onSuccess(userRegistry.createUser(user)) { performed =>
                    complete((StatusCodes.Created, performed))
                  }
                }
              })
          },
          path(Segment) { name =>
            concat(
              get {
                rejectEmptyResponse {
                  onSuccess(userRegistry.getUser(name)) { response =>
                    complete(response.maybeUser)
                  }
                }
              },
              delete {
                onSuccess(userRegistry.deleteUser(name)) { performed =>
                  complete((StatusCodes.OK, performed))
                }
              })
          })
      }
    }
  }
}
