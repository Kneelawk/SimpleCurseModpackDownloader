package org.kneelawk.simplecursemodpackdownloader.net

import scala.collection.mutable.MultiMap
import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.Task
import org.kneelawk.simplecursemodpackdownloader.EngineState
import org.kneelawk.simplecursemodpackdownloader.InterruptState

/**
 * Event sent when the download starts
 *
 * All headers are lower-case.
 */
case class DownloadStartedEvent(statusCode: Int, statusText: String, headers: MultiMap[String, String])
case class DownloadProgressEvent(current: Long, max: Long)

class DownloaderClient extends NetworkClient {
}

class DownloadTask extends Task {
  def addChild(child: Task): Unit = ???
  def getLastUpdateTime: Long = ???
  def getState: EngineState = ???
  def interrupt(state: InterruptState): Unit = ???
  def startTask: Unit = ???
}