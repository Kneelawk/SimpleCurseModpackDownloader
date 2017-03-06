package org.kneelawk.simplecursemodpackdownloader

import org.json4s.JsonMethods
import java.io.File
import com.ning.http.client.AsyncHandler
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpResponseStatus
import java.io.FileOutputStream

object SimpleDownloader {
  def apply(args: Array[String]) {
    import dispatch._, Defaults._
    import org.json4s._, JsonDSL._, jackson.JsonMethods._
    if (args.length < 5) {
      println("args: simple <username> <password> <projectId> <fileId>")
      return
    }
    val username = args(1)
    val password = args(2)
    val projectId = args(3).toInt
    val fileId = args(4).toInt
    val client = Http.configure(_.setFollowRedirect(true))
    try {
      println("Getting mod download...")
      val futDownload = Future({
        val futAuth = CurseUtils.auth(client, username, password)
        println("Authenticating...")
        for (fail <- futAuth.failed) {
          fail.printStackTrace()
          client.shutdown()
          Http.shutdown()
        }
        val auth = futAuth()
        val futModFile = CurseUtils.getAddonFile(client, auth.authToken, projectId, fileId)
        println("Getting File Info...")
        for (fail <- futModFile.failed) {
          fail.printStackTrace()
          client.shutdown()
          Http.shutdown()
        }
        futModFile()
      })
      for (file <- futDownload) {
        val outDir = new File("output")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = new File(outDir, file.diskFileName)
        val fstream = new FileOutputStream(outFile)
        println(file.downloadUrl)
        val fileUrl = url(file.downloadUrl)
        val downloader = new AsyncHandler[File] {
          var size = 0
          var downloaded = 0
          def onBodyPartReceived(res: HttpResponseBodyPart): AsyncHandler.STATE = {
            val data = res.getBodyPartBytes
            fstream.write(data);
            downloaded += data.length
            println(downloaded + " / " + size)
            AsyncHandler.STATE.CONTINUE
          }
          def onCompleted(): File = outFile
          def onHeadersReceived(res: HttpResponseHeaders): AsyncHandler.STATE = {
            val headers = res.getHeaders
            size = headers.getFirstValue("Content-Length").toInt
            AsyncHandler.STATE.CONTINUE
          }
          def onStatusReceived(res: HttpResponseStatus): AsyncHandler.STATE = {
            if (res.getStatusCode / 100 == 2) AsyncHandler.STATE.CONTINUE else AsyncHandler.STATE.ABORT
          }
          def onThrowable(t: Throwable): Unit = {
            t.printStackTrace()
            client.shutdown()
            Http.shutdown()
          }
        }
        val fileFut = client(fileUrl > downloader)
        for (a <- fileFut) {
          fstream.close()
          println("Done.")
          client.shutdown()
          Http.shutdown()
        }
      }
    } catch {
      case _: Exception =>
        client.shutdown()
        Http.shutdown()
    }
  }
}