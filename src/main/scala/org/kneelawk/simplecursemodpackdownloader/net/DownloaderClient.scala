package org.kneelawk.simplecursemodpackdownloader.net

import java.io.File

import scala.collection.mutable.MultiMap

import org.apache.http.client.methods.HttpUriRequest
import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.task.AbstractTask
import org.kneelawk.simplecursemodpackdownloader.task.InterruptState
import org.kneelawk.simplecursemodpackdownloader.task.TaskBuilder
import org.kneelawk.simplecursemodpackdownloader.task.TaskEventBus

/**
 * Event sent when the download starts
 *
 * All headers are lower-case.
 */
case class DownloadStartedEvent(req: HttpUriRequest, file: File, statusCode: Int, statusText: String, headers: MultiMap[String, String])
case class DownloadProgressEvent(current: Long, max: Long)
case class DownloadCompleteEvent(file: File, size: Long)

class DownloadTaskBuilder(netCtx: NetworkContext) extends TaskBuilder {
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

  def build(): DownloadTask = {
    if (req == null) throw new IllegalStateException("The request has not been set")
    if (file == null) throw new IllegalStateException("The file has not been set")

    return new DownloadTask(eventBus, req, file)
  }
}

class DownloadTask(eventBus: EventBus, req: HttpUriRequest, file: File) extends AbstractTask(eventBus) {
  protected def onInterrupt(state: InterruptState) {

  }

  def start() {

  }
}