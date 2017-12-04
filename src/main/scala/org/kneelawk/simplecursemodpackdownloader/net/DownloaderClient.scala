package org.kneelawk.simplecursemodpackdownloader.net

import scala.collection.mutable.MultiMap
import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.task.Task
import org.kneelawk.simplecursemodpackdownloader.task.EngineState
import org.kneelawk.simplecursemodpackdownloader.task.InterruptState
import org.kneelawk.simplecursemodpackdownloader.task.AbstractTask

/**
 * Event sent when the download starts
 *
 * All headers are lower-case.
 */
case class DownloadStartedEvent(statusCode: Int, statusText: String, headers: MultiMap[String, String])
case class DownloadProgressEvent(current: Long, max: Long)

class DownloaderClient extends NetworkClient {
}

class DownloadTask(eventBus: EventBus) extends AbstractTask(eventBus) {
  protected def onInterrupt(state: InterruptState) {
    
  }
  
  def startTask() {
    
  }
}