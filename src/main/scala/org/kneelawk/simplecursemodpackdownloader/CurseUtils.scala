package org.kneelawk.simplecursemodpackdownloader

import dispatch._, Defaults._
import org.json4s._, JsonDSL._, jackson.JsonMethods._
import java.util.Date
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.ExecutionException
import java.io.IOException

case class UserData(username: String, userId: Int, email: String, authToken: String)

case class ModAttachment(description: String, isDefault: Boolean, url: String)
case class ModAuthor(name: String, url: String)
case class ModCategory(id: Int, name: String, url: String)
case class ModCategorySection(gameId: Int, id: Int, name: String, packageType: String, path: String)
case class ModGameVersionLatestFile(fileType: String, gameVersion: String, fileId: Int, fileName: String)
case class ModFileDependency(addonId: Int, depType: String)
case class ModFileModule(fingerprint: Long, foldername: String)
case class ModFile(dependencies: List[ModFileDependency], downloadUrl: String, fileDate: Date,
  fileName: String, diskFileName: String, fileStatus: String, gameVersions: List[String], fileId: Int,
  isAvailable: Boolean, modules: List[ModFileModule], fingerprint: Long, releaseType: String)
case class ModData(attachments: List[ModAttachment], authors: List[ModAuthor], categories: List[ModCategory],
  categorySection: ModCategorySection, defaultFileId: Int, gameId: Int,
  gameVersionLatestFiles: List[ModGameVersionLatestFile], id: Int, latestFiles: List[ModFile], name: String,
  packageType: String, primaryCategoryId: Int, summary: String, websiteUrl: String)

object UserData {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): UserData = {
    val username = (x \ "username").extract[String]
    val userId = (x \ "user_id").extract[Int]
    val email = (x \ "email_address").extract[String]
    val token = (x \ "token").extract[String]
    val authToken = "Token " + userId + ":" + token
    return new UserData(username, userId, email, authToken)
  }
}

object ModAttachment {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModAttachment = {
    val description = (x \ "description").extract[String]
    val isDefault = (x \ "is_default").extract[Boolean]
    val url = (x \ "url").extract[String]
    return new ModAttachment(description, isDefault, url)
  }
}

object ModAuthor {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModAuthor = {
    val name = (x \ "name").extract[String]
    val url = (x \ "url").extract[String]
    return new ModAuthor(name, url)
  }
}

object ModCategory {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModCategory = {
    val id = (x \ "id").extract[Int]
    val name = (x \ "name").extract[String]
    val url = (x \ "url").extract[String]
    return new ModCategory(id, name, url)
  }
}

object ModCategorySection {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModCategorySection = {
    val gameId = (x \ "game_id").extract[Int]
    val id = (x \ "id").extract[Int]
    val name = (x \ "name").extract[String]
    val packageType = (x \ "package_type").extract[String]
    val path = (x \ "path").extract[String]
    return new ModCategorySection(gameId, id, name, packageType, path)
  }
}

object ModGameVersionLatestFile {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModGameVersionLatestFile = {
    val fileType = (x \ "file_type").extract[String]
    val gameVersion = (x \ "game_version").extract[String]
    val fileId = (x \ "project_file_id").extract[Int]
    val fileName = (x \ "project_file_name").extract[String]
    return new ModGameVersionLatestFile(fileType, gameVersion, fileId, fileName)
  }
}

object ModFileDependency {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModFileDependency = {
    val addonId = (x \ "add_on_id").extract[Int]
    val depType = (x \ "type").extract[String]
    return new ModFileDependency(addonId, depType)
  }
}

object ModFileModule {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModFileModule = {
    val fingerprint = (x \ "fingerprint").extract[Long]
    val foldername = (x \ "foldername").extract[String]
    return new ModFileModule(fingerprint, foldername)
  }
}

object ModFile {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModFile = {
    val dependencies = for (dep <- (x \ "dependencies").children) yield ModFileDependency(dep)
    val downloadUrl = (x \ "download_url").extract[String]
    val fileDate = CurseUtils.curseDateFormat.parse((x \ "file_date").extract[String])
    val fileName = (x \ "file_name").extract[String]
    val diskFileName = (x \ "file_name_on_disk").extract[String]
    val fileStatus = (x \ "file_status").extract[String]
    val gameVersions = for (version <- (x \ "game_version").children) yield version.extract[String]
    val fileId = (x \ "id").extract[Int]
    val isAvailable = (x \ "is_available").extract[Boolean]
    val modules = for (module <- (x \ "modules").children) yield ModFileModule(module)
    val fingerprint = (x \ "package_fingerprint").extract[Long]
    val releaseType = (x \ "release_type").extract[String]
    return new ModFile(dependencies, downloadUrl, fileDate, fileName, diskFileName, fileStatus,
      gameVersions, fileId, isAvailable, modules, fingerprint, releaseType)
  }
}

object ModData {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModData = {
    val attachments = for (attach <- (x \ "attachments").children) yield ModAttachment(attach)
    val authors = for (author <- (x \ "authors").children) yield ModAuthor(author)
    val categories = for (category <- (x \ "categories").children) yield ModCategory(category)
    val categorySection = ModCategorySection(x \ "category_section")
    val defaultFileId = (x \ "default_file_id").extract[Int]
    val gameId = (x \ "game_id").extract[Int]
    val gameVersionLatestFiles =
      for (file <- (x \ "game_version_latest_files").children) yield ModGameVersionLatestFile(file)
    val id = (x \ "id").extract[Int]
    val latestFiles = for (file <- (x \ "latest_files").children) yield ModFile(file)
    val name = (x \ "name").extract[String]
    val packageType = (x \ "package_type").extract[String]
    val primaryCategoryId = (x \ "primary_category_id").extract[Int]
    val summary = (x \ "summary").extract[String]
    val websiteUrl = (x \ "web_site_url").extract[String]
    return ModData(attachments, authors, categories, categorySection, defaultFileId, gameId,
      gameVersionLatestFiles, id, latestFiles, name, packageType, primaryCategoryId, summary, websiteUrl)
  }
}

case class NoFileForMinecraftVersionException(minecraftVersion: String)
  extends Exception("No mod file for minecraft " + minecraftVersion)

object CurseUtils {
  private implicit val formats = DefaultFormats

  val curseDateFormat = {
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    f.setTimeZone(TimeZone.getTimeZone("UTC"))
    f
  }

  val baseReq = url("http://curse-rest-proxy.azurewebsites.net/api")

  def authReq(username: String, password: String): Req = {
    var req = (baseReq / "authenticate").POST
      .addHeader("Content-Type", "application/json")
    req <<= compact(render(("username" -> username) ~ ("password" -> password)))
    return req
  }

  def addonReq(authToken: String, id: Int): Req = (baseReq / "addon" / id).addHeader("Authorization", authToken)

  def addonFilesReq(authToken: String, id: Int): Req = (baseReq / "addon" / id / "files")
    .addHeader("Authorization", authToken)

  def addonFileReq(authToken: String, id: Int, fileId: Int): Req = (baseReq / "addon" / id / "file" / fileId)
    .addHeader("Authorization", authToken)

  def auth(client: Http, username: String, password: String): Future[UserData] = {
    return client(authReq(username, password) OK as.json4s.Json).map(x => UserData(x \ "session"))
  }

  def getAddon(client: Http, authToken: String, id: Int): Future[ModData] = {
    return client(addonReq(authToken, id) OK as.json4s.Json).map(x => ModData(x))
  }

  def getAddonFiles(client: Http, authToken: String, id: Int): Future[List[ModFile]] = {
    return client(addonFilesReq(authToken, id) OK as.json4s.Json).map(x => {
      for (file <- (x \ "files").children) yield ModFile(file)
    })
  }

  def getAddonFile(client: Http, authToken: String, id: Int, fileId: Int): Future[ModFile] = {
    return client(addonFileReq(authToken, id, fileId) OK as.json4s.Json).map(x => ModFile(x))
  }

  def getLatestAddonFile(client: Http, authToken: String, id: Int, minecraftVersion: String): Future[ModFile] = {
    return getAddonFiles(client, authToken, id)
      .map(l => {
        val corver = l.filter(_.gameVersions.contains(minecraftVersion))
        if (corver.isEmpty)
          throw new NoFileForMinecraftVersionException(minecraftVersion)
        corver.reduceLeft((a, b) => if (a.fileDate.compareTo(b.fileDate) >= 0) a else b)
      })
  }

  def getExistingAddonFile(client: Http, authToken: String, id: Int, fileId: Int, minecraftVersion: String): Future[ModFile] = {
    getAddonFile(client, authToken, id, fileId).recoverWith {
      case t: ExecutionException if t.getCause.isInstanceOf[StatusCode]
        && t.getCause.asInstanceOf[StatusCode].code == 404 => {
        getLatestAddonFile(client, authToken, id, minecraftVersion)
      }
      case t: StatusCode if t.code == 404 => {
        getLatestAddonFile(client, authToken, id, minecraftVersion)
      }
    }
  }
}