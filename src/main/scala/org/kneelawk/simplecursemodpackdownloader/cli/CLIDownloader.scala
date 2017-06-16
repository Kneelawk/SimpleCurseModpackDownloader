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

object CLIDownloader {
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
    val client = new Http(new AsyncHttpClient(config))
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
    FileUtils.copyFile(modpack.manifestFile, new File(outDir, "manifest.json"))
    FileUtils.copyDir(modpack.overridesDir, new File(outDir, modpack.overridesName))
  }
}