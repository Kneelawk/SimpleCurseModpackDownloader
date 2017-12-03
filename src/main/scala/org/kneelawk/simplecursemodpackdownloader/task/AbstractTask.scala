package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.ListBuffer
import org.kneelawk.simplecursemodpackdownloader.EngineState
import org.kneelawk.simplecursemodpackdownloader.InterruptState

abstract class AbstractTask extends Task {
  protected val children = new ListBuffer[Task]
  protected var state: EngineState = EngineState.NotStarted
  protected var lastUpdate: Long = 0

  def addChild(task: Task) {
    children += task
  }

  def getLastUpdateTime = lastUpdate
  
  def getState = state
  
  def interrupt(state: InterruptState) {
    children.foreach(_.interrupt(state))
    onInterrupt(state)
  }
  
  protected def onInterrupt(state: InterruptState)

  protected def update() {
    lastUpdate = System.currentTimeMillis()
  }

  protected def setState(state: EngineState) = this.state = state
}