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

case class DownloadStarted(statusCode: Int, statusText: String)
case class DownloadHeaders(headers: FluentCaseInsensitiveStringsMap, isTrailing: Boolean)
case class DownloadProgress(maxSize: Long, downloaded: Long)
case class DownloadComplete(file: File, size: Long)

class DownloadOperation(val file: File,
    downloadStarted: DownloadStarted => Unit,
    downloadHeaders: DownloadHeaders => Unit,
    downloadProgress: DownloadProgress => Unit,
    downloadError: Throwable => Unit) extends AsyncHandler[File] {
  val fos = new FileOutputStream(file)
  val channel = fos.getChannel

  var maxSize = -1l
  var downloaded = 0l

  def onBodyPartReceived(res: HttpResponseBodyPart): AsyncHandler.STATE = {
    downloaded += channel.write(res.getBodyByteBuffer)
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

class Download(client: Http, req: Req, file: File) {
  private var downloadStarted: DownloadStarted => Unit = null
  private var downloadHeaders: DownloadHeaders => Unit = null
  private var downloadProgress: DownloadProgress => Unit = null
  private var downloadError: Throwable => Unit = null

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

  def start = client(req > new DownloadOperation(file, downloadStarted, downloadHeaders, downloadProgress, downloadError))
}

object Downloader {
  def download(client: Http, url: String, file: File) = new Download(client, DUrl(url), file)
}