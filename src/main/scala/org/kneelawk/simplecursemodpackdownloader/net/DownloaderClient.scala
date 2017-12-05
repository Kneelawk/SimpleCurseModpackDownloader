package org.kneelawk.simplecursemodpackdownloader.net

import java.io.File

import scala.collection.mutable.MultiMap
import scala.reflect.runtime.{ universe => ru }

import org.apache.http.client.methods.HttpUriRequest
import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.event.EventBusTaskBuilder
import org.kneelawk.simplecursemodpackdownloader.event.TaskBuilderEventBus
import org.kneelawk.simplecursemodpackdownloader.task.AbstractTask
import org.kneelawk.simplecursemodpackdownloader.task.InterruptState
import org.kneelawk.simplecursemodpackdownloader.task.Task

/**
 * Event sent when the download starts
 *
 * All headers are lower-case.
 */
case class DownloadStartedEvent(req: HttpUriRequest, file: File, statusCode: Int, statusText: String, headers: MultiMap[String, String])
case class DownloadProgressEvent(current: Long, max: Long)
case class DownloadCompleteEvent(file: File, size: Long)

class DownloadEventBus(b: DownloadTaskBuilder) extends EventBus(List(
  ru.typeOf[DownloadStartedEvent],
  ru.typeOf[DownloadProgressEvent],
  ru.typeOf[DownloadCompleteEvent])) with TaskBuilderEventBus {
  def builder = b
}

class DownloadTaskBuilder(netCtx: NetworkContext) extends EventBusTaskBuilder {
  private val eventBus = new DownloadEventBus(this)
  private var parent: Task = null
  private var req: HttpUriRequest = null
  private var file: File = null

  def setParent(parent: Task): this.type = {
    this.parent = parent
    return this
  }

  def bus = eventBus

  def build(): DownloadTask = {
    if (req == null) throw new IllegalStateException("The request has not been set")
    if (file == null) throw new IllegalStateException("The file has not been set")

    val task = new DownloadTask(eventBus, req, file)
    if (parent != null) parent.addChild(task)
    return task
  }
}

class DownloadTask(eventBus: DownloadEventBus, req: HttpUriRequest, file: File) extends AbstractTask(eventBus) {
  protected def onInterrupt(state: InterruptState) {

  }

  def start() {

  }
}