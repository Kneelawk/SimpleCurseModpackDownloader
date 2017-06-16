package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.io.FileOutputStream

import com.ning.http.client.AsyncHandler
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseStatus

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.Req
import dispatch.{ url => DUrl }
import scala.concurrent.Future

case class DownloadStarted(statusCode: Int, statusText: String)
case class DownloadHeaders(headers: FluentCaseInsensitiveStringsMap, isTrailing: Boolean)
case class DownloadProgress(maxSize: Long, downloaded: Long)
case class DownloadComplete(file: File, size: Long)

/*
 * Structure unstable: considering different arguments in constructor vs start():
 * file likely belongs in constructor, req unsure, client likely belongs in start()
 * so other starters and download maintainers can use their own clients
 */
/**
 * Download: Used for downloading requests into files.
 * <pre>
 * val download = new Download(file, "http://www.google.com/")
 *     .onDownloadProgress(p => println(p.downloaded + " / " + p.maxSize))
 *     .onDownloadError(_.printStackTrace())
 * <br>
 * val fut = download.start(client)
 * fut.onComplete {
 *     case Success(file) => println("Done.")
 *     case Failure(thro) => thro.printStackTrace()
 * }
 * </pre>
 */
class Download(val file: File, val request: Req) {
  private var downloadStarted: DownloadStarted => Unit = null
  private var downloadHeaders: DownloadHeaders => Unit = null
  private var downloadProgress: DownloadProgress => Unit = null
  private var downloadError: Throwable => Unit = null
  
  def this(file: File, url: String) {
    this(file, DUrl(url))
  }

  def onDownloadStarted(callback: DownloadStarted => Unit): Download = {
    downloadStarted = callback
    this
  }

  def onDownloadHeaders(callback: DownloadHeaders => Unit): Download = {
    downloadHeaders = callback
    this
  }

  def onDownloadProgress(callback: DownloadProgress => Unit): Download = {
    downloadProgress = callback
    this
  }

  def onDownloadError(callback: Throwable => Unit): Download = {
    downloadError = callback
    this
  }

  def start(client: Http) = client(request > DownloadOperation)
  
  def listTimeReceived = DownloadOperation.lastTimeReceived

  object DownloadOperation extends AsyncHandler[File] {
    val fos = new FileOutputStream(file)
    val channel = fos.getChannel

    var maxSize = -1l
    var downloaded = 0l
    var lastTimeReceived = System.currentTimeMillis()

    def onBodyPartReceived(res: HttpResponseBodyPart): AsyncHandler.STATE = {
      downloaded += channel.write(res.getBodyByteBuffer)
      lastTimeReceived = System.currentTimeMillis()
      if (downloadProgress != null)
        downloadProgress(DownloadProgress(maxSize, downloaded))
      AsyncHandler.STATE.CONTINUE
    }

    def onCompleted(): File = {
      fos.close()
      file
    }

    def onHeadersReceived(res: HttpResponseHeaders): AsyncHandler.STATE = {
      val headers = res.getHeaders
      maxSize = headers.getFirstValue("Content-Length").toInt
      downloaded = 0
      lastTimeReceived = System.currentTimeMillis()
      if (downloadHeaders != null)
        downloadHeaders(DownloadHeaders(headers, res.isTraillingHeadersReceived()))
      AsyncHandler.STATE.CONTINUE
    }

    def onStatusReceived(res: HttpResponseStatus): AsyncHandler.STATE = {
      val code = res.getStatusCode
      if (downloadStarted != null)
        downloadStarted(DownloadStarted(code, res.getStatusText))
      if (code / 100 == 2) AsyncHandler.STATE.CONTINUE else AsyncHandler.STATE.ABORT
    }

    def onThrowable(t: Throwable): Unit = {
      if (downloadError != null)
        downloadError(t)
    }
  }
}