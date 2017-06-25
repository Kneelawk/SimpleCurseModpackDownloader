package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.io.InputStream

import org.json4s.DefaultFormats
import org.json4s.JValue
import org.json4s.file2JsonInput
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jvalue2extractable
import org.json4s.jvalue2monadic
import org.json4s.stream2JsonInput
import org.kneelawk.simplecursemodpackdownloader.io.FileUtils

case class ModpackManifestMinecraftModLoader(id: String, primary: Boolean)
case class ModpackManifestMinecraft(version: String, modLoaders: List[ModpackManifestMinecraftModLoader])
case class ModpackManifestFile(projectId: Int, fileId: Int, required: Boolean)
case class ModpackManifest(minecraft: ModpackManifestMinecraft, manifestType: String, manifestVersion: Int,
  name: String, version: String, author: String, files: List[ModpackManifestFile], overrides: String)

object ModpackManifestMinecraftModLoader {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModpackManifestMinecraftModLoader = {
    val id = (x \ "id").extract[String]
    val primary = (x \ "primary").extract[Boolean]
    return new ModpackManifestMinecraftModLoader(id, primary)
  }
}

object ModpackManifestMinecraft {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModpackManifestMinecraft = {
    val version = (x \ "version").extract[String]
    val modLoaders = for (modLoader <- (x \ "modLoaders").children) yield ModpackManifestMinecraftModLoader(modLoader)
    return new ModpackManifestMinecraft(version, modLoaders)
  }
}

object ModpackManifestFile {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModpackManifestFile = {
    val projectId = (x \ "projectID").extract[Int]
    val fileId = (x \ "fileID").extract[Int]
    val required = (x \ "required").extract[Boolean]
    return new ModpackManifestFile(projectId, fileId, required)
  }
}

object ModpackManifest {
  private implicit val formats = DefaultFormats
  def apply(x: JValue): ModpackManifest = {
    val minecraft = ModpackManifestMinecraft(x \ "minecraft")
    val manifestType = (x \ "manifestType").extract[String]
    val manifestVersion = (x \ "manifestVersion").extract[Int]
    val name = (x \ "name").extract[String]
    val version = (x \ "version").extract[String]
    val author = (x \ "author").extract[String]
    val files = for (file <- (x \ "files").children) yield ModpackManifestFile(file)
    val overrides = (x \ "overrides").extract[String]
    return new ModpackManifest(minecraft, manifestType, manifestVersion, name, version, author, files, overrides)
  }
}

object ManifestUtils {
  def getManifest(is: InputStream) = ModpackManifest(parse(is))
  def getManifest(file: File) = ModpackManifest(parse(file))
}

class Modpack(val tmpDir: File, val manifestFile: File, val manifest: ModpackManifest) {
  val overridesName = manifest.overrides
  val overridesDir = new File(tmpDir, overridesName)
}

object Modpack {
  def load(modpackZip: File): Modpack = {
    val tmpDir = FileUtils.unzipZip(modpackZip)
    val manifestFile = new File(tmpDir, "manifest.json")
    val manifest = ManifestUtils.getManifest(manifestFile)
    return new Modpack(tmpDir, manifestFile, manifest)
  }
}