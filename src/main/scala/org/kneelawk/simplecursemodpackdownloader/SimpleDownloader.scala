package org.kneelawk.simplecursemodpackdownloader

import java.io.File

import org.apache.http.impl.nio.client.HttpAsyncClients
import org.kneelawk.simplecursemodpackdownloader.curse.CurseUtils
import org.kneelawk.simplecursemodpackdownloader.net.URIUtil

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig

import dispatch.Defaults.executor
import dispatch.Future
import dispatch.Http
import dispatch.enrichFuture
import org.kneelawk.simplecursemodpackdownloader.net.NetworkContext
import org.kneelawk.simplecursemodpackdownloader.task.TaskManifest
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadTaskBuilder
import org.apache.http.client.methods.HttpGet
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadProgressEvent
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadFailedEvent
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadCompleteEvent

object SimpleDownloader {
  def apply(args: Array[String]) {
    if (args.length < 5) {
      println("args: simple <username> <password> <projectId> <fileId>")
      return
    }
    val username = args(1)
    val password = args(2)
    val projectId = args(3).toInt
    val fileId = args(4).toInt
    val config = new AsyncHttpClientConfig.Builder()
      .setUserAgent("CurseModpackDownloader/0.0.1")
      .setRequestTimeout(-1)
      .setFollowRedirect(true)
      .build()
    val client = new Http(new AsyncHttpClient(config))
    val netCtx = NetworkContext()
    netCtx.start()
    try {
      println("Getting mod download...")
      val futDownload = Future({
        val futAuth = CurseUtils.auth(client, username, password)
        println("Authenticating...")
        for (fail <- futAuth.failed) {
          fail.printStackTrace()
          client.shutdown()
        }
        val auth = futAuth()
        val futModFile = CurseUtils.getAddonFile(client, auth.authToken, projectId, fileId)
        println("Getting File Info...")
        for (fail <- futModFile.failed) {
          fail.printStackTrace()
          client.shutdown()
        }
        futModFile()
      })
      for (file <- futDownload) {
        val outDir = new File("output")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = new File(outDir, file.diskFileName)
        println(file.downloadUrl)
        val sanitaryUri = URIUtil.sanitizeCurseDownloadUri(file.downloadUrl, true)
        new FileDownloadTaskBuilder(netCtx).setFile(outFile).setRequest(new HttpGet(sanitaryUri)).getBus
          .register((e: FileDownloadProgressEvent) => println(e.current + " / " + e.max))
          .register((e: FileDownloadFailedEvent) => {
            e.exception.printStackTrace()
            client.shutdown()
            netCtx.close()
          })
          .register((e: FileDownloadCompleteEvent) => {
            println("Done.")
            client.shutdown()
            netCtx.close()
          })
          .builder.build().start()
      }
    } catch {
      case _: Exception =>
        client.shutdown()
//        downloadClient.close()
        netCtx.close()
    }
  }
}