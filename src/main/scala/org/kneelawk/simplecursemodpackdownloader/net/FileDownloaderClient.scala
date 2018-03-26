package org.kneelawk.simplecursemodpackdownloader.net

import java.io.File

import scala.collection.mutable.MultiMap

import org.apache.http.client.methods.HttpUriRequest
import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.task.AbstractTask
import org.kneelawk.simplecursemodpackdownloader.task.InterruptState
import org.kneelawk.simplecursemodpackdownloader.task.TaskBuilder
import org.kneelawk.simplecursemodpackdownloader.task.TaskEventBus
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer
import org.apache.http.protocol.HttpContext
import org.apache.http.nio.ContentDecoder
import org.apache.http.nio.IOControl
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.HttpResponse
import org.apache.http.nio.client.methods.HttpAsyncMethods
import org.apache.http.concurrent.FutureCallback
import org.kneelawk.simplecursemodpackdownloader.task.EngineState
import java.util.concurrent.{ Future => JFuture }
import scala.collection.mutable.HashMap
import scala.collection.mutable.Set
import java.io.IOException
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Event sent when the download starts
 *
 * All headers are lower-case.
 */
case class FileDownloadStartedEvent(req: HttpUriRequest, file: File, statusCode: Int,
    statusText: String, headers: MultiMap[String, String]) {
  private var invalidResponse: InvalidResponseType = InvalidResponseType.None

  /**
   * Stop the download because of an invalid response.
   * Will eventually set the download task's state to crashed so it can be restarted.
   * Is only effective in the scope of this callback.
   */
  def setInvalidResponseRecoverable() =
    if (invalidResponse == InvalidResponseType.None) invalidResponse = InvalidResponseType.Recoverable

  /**
   * Stop the download because of an invalid response.
   * Will eventually set the download task's state to dead so it will not be restarted.
   * Is only effective in the scope of this callback.
   */
  def setInvalidResponseFatal() = invalidResponse = InvalidResponseType.Fatal

  def getInvalidResponseType = invalidResponse
}

/**
 * Event sent every time new content is received.
 */
case class FileDownloadProgressEvent(current: Long, max: Long)

/**
 * Event sent when the download completed successfully.
 */
case class FileDownloadCompleteEvent(file: File, size: Long)

/**
 * Event sent if the download failed.
 */
case class FileDownloadFailedEvent(file: File, exception: Exception)

/**
 * Builds a DownloadTask.
 */
class FileDownloadTaskBuilder(netCtx: NetworkContext) extends TaskBuilder {
  private val eventBus = new TaskEventBus(this)
  private var req: HttpUriRequest = null
  private var file: File = null

  def setRequest(r: HttpUriRequest): this.type = {
    req = r
    this
  }

  def setFile(f: File): this.type = {
    file = f
    this
  }

  def getBus = eventBus

  def build(): FileDownloadTask = {
    if (req == null) throw new IllegalStateException("The request has not been set")
    if (file == null) throw new IllegalStateException("The file has not been set")

    return new FileDownloadTask(netCtx, eventBus, req, file)
  }
}

/**
 * The download task.
 */
class FileDownloadTask(netCtx: NetworkContext, eventBus: EventBus, req: HttpUriRequest, file: File) extends AbstractTask(eventBus) {
  var interruptState: InterruptState = null
  var downloadFuture: JFuture[File] = null

  /**
   * Handles interrupts within this task.
   */
  protected def onInterrupt(state: InterruptState) {
    interruptState = state

    state match {
      case InterruptState.Abort => setState(EngineState.Aborting)
      case InterruptState.ZombieKill => setState(EngineState.ZombieKilling)
    }

    if (downloadFuture != null) {
      downloadFuture.cancel(true)
    }
  }

  /**
   * Starts the download.
   * Can be called multiple times if needed.
   */
  def start() {
    update()

    interruptState = null

    val operation = new DownloadOperation

    downloadFuture = netCtx.client.execute(HttpAsyncMethods.create(req), operation, new FutureCallback[File] {
      /**
       * Callback for if the download is canceled.
       * I assume this means if downloadFuture.cancel(true)
       */
      def cancelled() {
        if (interruptState != null) {
          interruptState match {
            case InterruptState.Abort => setState(EngineState.Aborted)
            case InterruptState.ZombieKill => setState(EngineState.ZombieKilled)
          }
        }
      }

      /**
       * Callback for if the download completes successfully.
       */
      def completed(file: File) {
        setState(EngineState.Finished)

        getBus.sendEvent(FileDownloadCompleteEvent(file, operation.downloaded))
      }

      /**
       * Callback for if the download fails for some reason.
       */
      def failed(ex: Exception) {
        if (ex.isInstanceOf[InvalidResponseException]) {
          if (ex.asInstanceOf[InvalidResponseException].tpe == InvalidResponseType.Fatal) {
            setState(EngineState.Dead)
          } else {
            setState(EngineState.Crashed)
          }
        } else {
          setState(EngineState.Crashed)
        }

        getBus.sendEvent(FileDownloadFailedEvent(file, ex))
      }
    })
  }

  /**
   * Internal state of each download operation.
   */
  private class DownloadOperation extends AbstractAsyncResponseConsumer[File] {
    val fos = new FileOutputStream(file)
    val channel = fos.getChannel
    val buf = ByteBuffer.allocate(8192)

    var maxSize = -1l
    var downloaded = 0l

    /**
     * When the download starts.
     */
    protected def onResponseReceived(res: HttpResponse) {
      update()

      val code = res.getStatusLine.getStatusCode

      val event = FileDownloadStartedEvent(req, file, code, res.getStatusLine.getReasonPhrase, res.getAllHeaders.foldLeft(
        new HashMap[String, Set[String]] with MultiMap[String, String])(
          (acc, header) => acc.addBinding(header.getName.toLowerCase(), header.getValue)))

      getBus.sendEvent(event)

      if (event.getInvalidResponseType != InvalidResponseType.None) {
        throw new InvalidResponseException(req, event.getInvalidResponseType, code, event.statusText, event.headers)
      }
    }

    /**
     * When enough information about the content has arrived.
     */
    protected def onEntityEnclosed(entity: HttpEntity, ct: ContentType) {
      update()

      maxSize = entity.getContentLength
    }

    /**
     * When each piece of the content arrives.
     */
    protected def onContentReceived(cd: ContentDecoder, io: IOControl) {
      update()

      buf.clear()

      while (cd.read(buf) > 0 || buf.position() != 0) {
        buf.flip()
        downloaded = channel.write(buf)
        buf.compact()
      }

      getBus.sendEvent(FileDownloadProgressEvent(downloaded, maxSize))
    }

    /**
     * Literally just returns the file for the download.
     */
    protected def buildResult(ctx: HttpContext): File = file

    /**
     * Closes the FileOutputStream and through it, the FileChannel.
     */
    protected def releaseResources() {
      fos.close()
    }
  }
}
