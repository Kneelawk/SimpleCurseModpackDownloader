package org.kneelawk.simplecursemodpackdownloader.task

import java.util.concurrent.locks.ReentrantLock

import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.util.LockUtil.lock
import org.kneelawk.simplecursemodpackdownloader.util.LockUtil.tryLock

abstract class AbstractTask(eventBus: EventBus)(implicit protected val manifest: TaskManifest) extends Task {

  protected val children = new TaskManifest
  @volatile protected var state: EngineState = EngineState.NotStarted
  @volatile protected var lastUpdate: Long = 0

  override def addChild(task: Task) {
    if (task.getState != EngineState.NotStarted) {
      throw new IllegalStateException("A task cannot be running when added to a parent")
    }

    task.getBus.register((e: TaskStateChangeEvent) => {
      if (!e.task.isAllive) {
        children -= e.task
      }
    })

    children += task

    manifest += task
  }

  def getBus = eventBus

  def getLastUpdateTime = lastUpdate

  def getState = state

  def interrupt(state: InterruptState) {
    // no more need for a lock when iterating over an immutable copy of children
    children.foreach(_.interrupt(state))
    children.pruneTasks()

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