package org.kneelawk.simplecursemodpackdownloader.cli

import java.io.File
import java.io.IOException

import scala.util.Failure
import scala.util.Success

import org.kneelawk.simplecursemodpackdownloader.CurseUtils
import org.kneelawk.simplecursemodpackdownloader.FileUtils
import org.kneelawk.simplecursemodpackdownloader.Modpack
import org.kneelawk.simplecursemodpackdownloader.UserData
import org.kneelawk.simplecursemodpackdownloader.console.ConsoleInterfaceFactory

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig

import dispatch.Defaults.executor
import dispatch.Http
import org.kneelawk.simplecursemodpackdownloader.ModProgressListener
import org.kneelawk.simplecursemodpackdownloader.ModFile
import org.kneelawk.simplecursemodpackdownloader.ModpackProgressListener
import org.kneelawk.simplecursemodpackdownloader.ModpackEngine

object CLIDownloader {
  var client: Http = null
  def apply(args: Array[String]) {
    if (args.length < 3) {
      println("cli <input-zip> <output-dir>")
      return
    }

    val console = ConsoleInterfaceFactory.getConsoleInterface
    if (console == null)
      throw new IOException("The cli version of the application should be run from the console.")

    val username = console.prompt("Username: ")
    val password = new String(console.promptHiddenInput("Password: "))

    val modpackFile = new File(args(1))
    val outDir = new File(args(2))

    println("Logging in...")

    val config = new AsyncHttpClientConfig.Builder()
      .setUserAgent("CurseModpackDownloader/0.0.1")
      .setRequestTimeout(-1)
      .setFollowRedirect(true)
      .build()
    client = new Http(new AsyncHttpClient(config))
    val futUser = CurseUtils.auth(client, username, password)
    futUser.onComplete {
      case Success(user) => {
        println("Logged in as " + user.username)
        beginExtraction(user, modpackFile, outDir)
      }
      case Failure(thro) => {
        println("Unable to authenticate")
        client.shutdown()
      }
    }
  }

  def beginExtraction(user: UserData, modpackFile: File, outDir: File) {
    try {
      outDir.mkdirs()

      println()
      println("Loading modpack...")
      val modpack = Modpack.load(modpackFile)
      println("Modpack name: " + modpack.manifest.name)
      println("Modpack version: " + modpack.manifest.version)
      println("Modpack author: " + modpack.manifest.author)
      println("Minecraft version: " + modpack.manifest.minecraft.version)
      println()
      println("Mod loaders:")
      modpack.manifest.minecraft.modLoaders.foreach { l =>
        println('\t' + l.id + (if (l.primary) " - primary" else ""))
      }
      println()

      println("Copying overrides...")
      FileUtils.copyFile(modpack.manifestFile, new File(outDir, "manifest.json"), true)
      FileUtils.copyDir(modpack.overridesDir, outDir)

      println("Downloading files...")
      object ModpackListener extends ModpackProgressListener {
        def createModProgressListener(projectId: Int, fileId: Int) = new ModListener(projectId, fileId)
        def onOverallProgress(current: Long, max: Long) = println(s"## Completed Mod download: $current / $max")
        def onAllModsComplete {
          println("## Mod Downloads Complete")
          terminate
        }
        def onDeath {
          println("Fatal error")
          terminate
        }
        def onAbort {
          println("Download aborted")
          terminate
        }
      }

      val engine = new ModpackEngine(client, user.authToken, modpack.manifest, new File(outDir, "mods"), ModpackListener)
      engine.start
    } catch {
      case _: Exception => terminate
    }
  }

  class ModListener(projectId: Int, fileId: Int) extends ModProgressListener {
    var m: ModFile = null
    var lastProgress = 0l

    def onBeginResolvingMod = println(s"Starting resolve: $projectId")

    def onModResolved(mod: ModFile) = m = mod

    def onBeginModDownload = println(s"Starting download: ${m.fileName}")

    def onModDownloadProgress(current: Long, max: Long) {
      if (current > lastProgress + 1000) {
        println(s"Downloading ${m.fileName} ${current * 100 / max}%")
      }
    }

    def onCompletedModDownload {
      println(s"Finished downlading ${m.fileName}")
    }

    def onError(t: Throwable) {
      println(s"Error while downloading ${if (m != null) m.fileName else projectId}:")
      println(t.toString())
    }

    def onCrash(t: Throwable) {
      println(s"Recoverable error while downloading ${if (m != null) m.fileName else projectId}:")
      println(t.toString())
    }

    def onDeath(t: Throwable) {
      println(s"Fatal error while downlading ${if (m != null) m.fileName else projectId}:")
      println(t.toString())
    }

    def onAbort {
      println(s"Aborted ${if (m != null) m.fileName else projectId}")
    }
  }

  def terminate {
    client.shutdown()
  }
}