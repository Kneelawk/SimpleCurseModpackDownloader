package org.kneelawk.simplecursemodpackdownloader

import dispatch._, Defaults._
import org.json4s._, JsonDSL._, jackson.JsonMethods._
import java.util.Date

case class UserData(username: String, userId: Int, email: String, authToken: String)

case class ModAttachment(description: String, isDefault: Boolean, url: String)
case class ModAuthor(name: String, url: String)
case class ModCategory(id: Int, name: String, url: String)
case class ModCategorySection(gameId: Int, id: Int, name: String, packageType: String, path: String)
case class ModGameVersionLatestFile(fileType: String, gameVersion: String, fileId: Int, fileName: String)
case class ModFileDependency(addonId: Int, depType: String)
case class ModFileModule(fingerprint: Long, foldername: String)
case class ModFile(dependencies: List[ModFileDependency], downloadUrl: String, fileDate: Date,
  fileName: String, fileStatus: String, gameVersions: List[String], id: Int, isAvailable: Boolean,
  modules: List[ModFileModule], fingerprint: Long, releaseType: String)
case class ModData(attachments: List[ModAttachment], authors: List[ModAuthor], categories: List[ModCategory],
  categorySection: ModCategorySection, defaultFileId: Int, gameId: Int,
  gameVersionLatestFiles: List[ModGameVersionLatestFile], id: Int, latestFiles: List[ModFile], name: String,
  packageType: String, primaryCategoryId: Int, summary: String, websiteUrl: String)

object CurseUtils {
  private implicit val formats = DefaultFormats

  val baseReq = url("curse-rest-proxy.azurewebsites.net/api")

  def authReq(username: String, password: String): Req = {
    var req = (baseReq / "authenticate").POST
      .addHeader("Content-Type", "application/json")
    req <<= compact(render(("username" -> username) ~ ("password" -> password)))
    return req
  }

  def addonReq(authToken: String, id: Int): Req = (baseReq / "addon" / id).addHeader("Authorizaion", authToken)

  def addonFilesReq(authToken: String, id: Int): Req = (baseReq / "addon" / id / "files")
    .addHeader("Authorization", authToken)

  def addonFileReq(authToken: String, id: Int, fileId: Int): Req = (baseReq / "addon" / id / "file" / fileId)
    .addHeader("Authorization", authToken)

  def auth(client: Http, username: String, password: String): Future[UserData] = {
    return client(authReq(username, password) OK as.json4s.Json).map { x =>
      val session = x \ "session"
      val userId = (session \ "user_id").extract[Int]
      val token = (session \ "token").extract[String]
      new UserData((session \ "username").extract[String], userId,
        (session \ "email_address").extract[String], "Token " + userId + ":" + token)
    }
  }

  def getAddon(client: Http, authToken: String, id: Int) = {
    // TODO add addon information retrieval
  }
}