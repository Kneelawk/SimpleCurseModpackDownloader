package org.kneelawk.simplecursemodpackdownloader.task

import scala.concurrent.ExecutionContext
import java.util.concurrent.CountDownLatch
import scala.concurrent.duration.Duration
import scala.concurrent.Future

class TaskHandler(child: Task, parent: Task, blocked: BlockableHandle)(implicit ctx: ExecutionContext,
    manifest: TaskManifest) {

  /**
   * A constructor that doesn't bother with the BlockableHandle.
   */
  def this(child: Task, parent: Task)(implicit ctx: ExecutionContext, manifest: TaskManifest) =
    this(child, parent, null)(ctx, manifest)

  private val done = new CountDownLatch(1)

  // register state change listeners on the task for restarting
  child.getBus.register((e: TaskStateChangeEvent) => {
    if (e.state.isInstanceOf[RestartableEngineState]) {
      startImpl()
    } else if (e.state.isInstanceOf[StoppedEngineState]) {
      done.countDown()

      // child task is done parent execution returns
      if (blocked != null)
        blocked.setBlocked(false)
    }
  })

  /**
   * Start the task in the supplied execution context and return immediately.
   *
   * This also adds the child task to the parent task and sets the blocked handle to true if it isn't null.
   */
  def start(): this.type = {
    // lets take care of this here too
    parent.addChild(child)

    manifest.addTask(child)

    // child task is blocking parent execution
    if (blocked != null)
      blocked.setBlocked(true)

    startImpl()

    this
  }

  private def startImpl() {
    Future {
      child.start()
    }
  }
  
  /**
   * Is this task done?
   */
  def isDone: Boolean = done.getCount == 0

  /**
   * Wait for the task to enter a StoppedEngineState.
   */
  def await() {
    done.await()
  }

  /**
   * Wait a specific amount of time for the task to enter a StoppedEngineState.
   */
  def await(d: Duration) {
    done.await(d.length, d.unit)
  }
}