package org.kneelawk.simplecursemodpackdownloader

import java.io.File

import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.AsyncHttpClientConfig

import dispatch.Defaults.executor
import dispatch.Future
import dispatch.Http
import dispatch.enrichFuture

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
        val fileFut = Downloader.download(client, file.downloadUrl, outFile)
          .onDownloadProgress(progress => println(progress.downloaded + " / " + progress.maxSize))
          .onDownloadError(_.printStackTrace())
          .start
        for (a <- fileFut) {
          println("Done.")
          client.shutdown()
        }
        for (t <- fileFut.failed) {
          t.printStackTrace()
          client.shutdown()
        }
      }
    } catch {
      case _: Exception =>
        client.shutdown()
    }
  }
}