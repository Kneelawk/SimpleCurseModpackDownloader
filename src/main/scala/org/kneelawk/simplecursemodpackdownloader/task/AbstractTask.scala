package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.ListBuffer

import org.kneelawk.simplecursemodpackdownloader.event.EventBus

abstract class AbstractTask(eventBus: EventBus) extends Task {
  // TODO do something with TaskManifests

  protected val children = new ListBuffer[Task]
  @volatile protected var state: EngineState = EngineState.NotStarted
  @volatile protected var lastUpdate: Long = 0

  override def addChild(task: Task) {
    if (task.getState != EngineState.NotStarted) {
      throw new IllegalStateException("A task cannot be running when added to a parent")
    }

    // FIXME this is a deadlock situation
    task.getBus.register((e: TaskStateChangeEvent) => if (!e.task.isAllive) children.synchronized(children -= e.task))

    children.synchronized(children += task)
  }

  def getBus = eventBus

  def getLastUpdateTime = lastUpdate

  def getState = state

  def interrupt(state: InterruptState) {
    // FIXME this is a deadlock situation
    children.synchronized(children.foreach(_.interrupt(state)))
    onInterrupt(state)
    eventBus.sendEvent(new TaskInterruptEvent(this, state))
  }

  protected def onInterrupt(state: InterruptState)

  protected def update() {
    lastUpdate = System.currentTimeMillis()
  }

  protected def setState(state: EngineState) = {
    this.state = state
    eventBus.sendEvent(new TaskStateChangeEvent(this, state))
  }
}