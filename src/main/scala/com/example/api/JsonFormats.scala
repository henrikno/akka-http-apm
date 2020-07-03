package com.example.api

import com.example.api.UserRegistry.ActionPerformed
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormats {

  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] = jsonFormat3(User)
  implicit val usersJsonFormat: RootJsonFormat[Users] = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed] = jsonFormat1(ActionPerformed)
}
