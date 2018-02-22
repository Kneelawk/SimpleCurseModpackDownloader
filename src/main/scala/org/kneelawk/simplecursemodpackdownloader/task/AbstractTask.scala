package org.kneelawk.simplecursemodpackdownloader.task

import java.util.concurrent.locks.ReentrantLock

import org.kneelawk.simplecursemodpackdownloader.event.EventBus
import org.kneelawk.simplecursemodpackdownloader.util.LockUtil.lock
import org.kneelawk.simplecursemodpackdownloader.util.LockUtil.tryLock

abstract class AbstractTask(eventBus: EventBus) extends Task {

  protected val children = new TaskManifest
  @volatile protected var state: EngineState = EngineState.NotStarted
  @volatile protected var lastUpdate: Long = 0
  @volatile protected var blocked: Boolean = false

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
  }

  override def getBus = eventBus

  override def getLastUpdateTime = lastUpdate

  override def getState = state
  
  override def isBlocked = blocked

  override def interrupt(state: InterruptState) {
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
  
  protected object BlockedHandle extends BlockableHandle {
    override def setBlocked(b: Boolean) = blocked = b
  }
}