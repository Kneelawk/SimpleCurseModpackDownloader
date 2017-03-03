package org.kneelawk.simplecursemodpackdownloader

import dispatch._, Defaults._
import org.json4s._, JsonDSL._, jackson.JsonMethods._

case class UserData(username: String, userId: Int, email: String, authToken: String)

object CurseConstants {
  val baseReq = url("curse-rest-proxy.azurewebsites.net/api")

  def authReq(username: String, password: String) = (baseReq / "authenticate").POST
    .addHeader("Content-Type", "application/json")
    .<<(compact(render(("username" -> username) ~ ("password" -> password))))

  def auth(username: String, password: String): Future[UserData] = {
    // TODO do auth stuff
    null
  }
}