package com.example.api

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import co.elastic.apm.api.ElasticApm
import com.example.api.UserRegistry._
import UserRegistry.GetUsers

import scala.collection.immutable
import scala.concurrent.Future

final case class User(name: String, age: Int, countryOfResidence: String)
final case class Users(users: immutable.Seq[User])

object UserRegistry {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(name: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(Set.empty)

  private def registry(users: Set[User]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        println("transaction-id: " +  ElasticApm.currentTransaction().getId + " UserRegistry.GetUsers " + " thread=" + Thread.currentThread().getId)
        replyTo ! Users(users.toSeq)
        Behaviors.same
      case CreateUser(user, replyTo) =>
        println("transaction-id: " +  ElasticApm.currentTransaction().getId + " UserRegistry.CreateUser " + " thread=" + Thread.currentThread().getId)
        replyTo ! ActionPerformed(s"User ${user.name} created.")
        registry(users + user)
      case GetUser(name, replyTo) =>
        println("transaction-id: " +  ElasticApm.currentTransaction().getId + " UserRegistry.GetUser " + " thread=" + Thread.currentThread().getId)
        replyTo ! GetUserResponse(users.find(_.name == name))
        Behaviors.same
      case DeleteUser(name, replyTo) =>
        println("transaction-id: " +  ElasticApm.currentTransaction().getId + " UserRegistry.DeleteUser " + " thread=" + Thread.currentThread().getId)
        replyTo ! ActionPerformed(s"User $name deleted.")
        registry(users.filterNot(_.name == name))
    }
}

class UserRegistryClient(val userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_], val timeout: Timeout) {

  def getUsers(): Future[Users] = {
    println("transaction-id: " +  ElasticApm.currentTransaction().getId + " UserRegistryClient.getUsers ask " + " thread=" + Thread.currentThread().getId)
    val reply = userRegistry.ask(GetUsers)
    println("transaction-id: " +  ElasticApm.currentTransaction().getId + " UserRegistryClient.getUsers reply " + " thread=" + Thread.currentThread().getId)
    reply
  }
  def getUser(name: String): Future[GetUserResponse] = userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] = userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] = userRegistry.ask(DeleteUser(name, _))

}
