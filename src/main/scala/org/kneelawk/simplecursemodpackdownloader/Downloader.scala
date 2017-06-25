package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.collection.mutable.Set

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.nio.ContentDecoder
import org.apache.http.nio.IOControl
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer
import org.apache.http.protocol.HttpContext
import org.apache.http.nio.client.methods.HttpAsyncMethods
import org.apache.http.concurrent.FutureCallback
import org.kneelawk.simplecursemodpackdownloader.net.StatusCodeException

case class DownloadStarted(statusCode: Int, statusText: String, headers: MultiMap[String, String])
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
 * @deprecated This downloader api is falling apart, please await the arrival of the NetworkClient system.
 */
@deprecated
class Download(val file: File, val request: HttpUriRequest) {
  private var downloadStarted: DownloadStarted => Unit = null
  private var downloadProgress: DownloadProgress => Unit = null
  private var downloadComplete: DownloadComplete => Unit = null
  private var downloadError: Throwable => Unit = null

  def this(file: File, url: String) {
    this(file, new HttpGet(url))
  }

  def onDownloadStarted(callback: DownloadStarted => Unit): Download = {
    downloadStarted = callback
    this
  }

  def onDownloadProgress(callback: DownloadProgress => Unit): Download = {
    downloadProgress = callback
    this
  }

  def onDownloadComplete(callback: DownloadComplete => Unit): Download = {
    downloadComplete = callback
    this
  }

  def onDownloadError(callback: Throwable => Unit): Download = {
    downloadError = callback
    this
  }

  def start(client: HttpAsyncClient): java.util.concurrent.Future[File] = {
    client.execute(HttpAsyncMethods.create(request), DownloadOperation, new FutureCallback[File] {
      def cancelled() {}
      def completed(file: File) {
        if (downloadComplete != null)
          downloadComplete(DownloadComplete(file, DownloadOperation.downloaded))
      }
      def failed(e: Exception) {
        if (downloadError != null)
          downloadError(e)
      }
    })
  }

  def listTimeReceived = DownloadOperation.lastTimeReceived

  object DownloadOperation extends AbstractAsyncResponseConsumer[File] {
    val fos = new FileOutputStream(file)
    val channel = fos.getChannel
    val buf = ByteBuffer.allocate(8192)

    var maxSize = -1l
    var downloaded = 0l
    var lastTimeReceived = System.currentTimeMillis()

    def onContentReceived(cd: ContentDecoder, ioc: IOControl) {
      buf.clear()
      while (cd.read(buf) > 0 || buf.position() != 0) {
        downloaded += buf.position()
        buf.flip()
        channel.write(buf)
        buf.compact()
      }
      lastTimeReceived = System.currentTimeMillis()
      if (downloadProgress != null)
        downloadProgress(DownloadProgress(maxSize, downloaded))
    }

    def buildResult(ctx: HttpContext): File = {
      file
    }

    def onEntityEnclosed(entity: HttpEntity, ct: ContentType) {
      maxSize = entity.getContentLength
      downloaded = 0
      lastTimeReceived = System.currentTimeMillis()
    }

    def onResponseReceived(res: HttpResponse) {
      val code = res.getStatusLine.getStatusCode
      lastTimeReceived = System.currentTimeMillis()

      if (downloadStarted != null)
        downloadStarted(DownloadStarted(code, res.getStatusLine.getReasonPhrase, res.getAllHeaders.foldLeft(
          new HashMap[String, Set[String]] with MultiMap[String, String])(
            (acc, header) => acc.addBinding(header.getName.toLowerCase(), header.getValue))))

      if (code / 100 != 2)
        throw new StatusCodeException(code, res.getStatusLine.getReasonPhrase)
    }

    def releaseResources() {
      fos.close()
    }
  }
}