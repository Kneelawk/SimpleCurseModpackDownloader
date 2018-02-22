package org.kneelawk.simplecursemodpackdownloader.task

import scala.concurrent.ExecutionContext
import java.util.concurrent.CountDownLatch
import scala.concurrent.duration.Duration
import scala.concurrent.Future

class TaskHandler(task: Task)(implicit ctx: ExecutionContext) {
  private val done = new CountDownLatch(1)
  
  // register state change listeners on the task for restarting
  task.getBus.register((e: TaskStateChangeEvent) => {
    if (e.state.isInstanceOf[RestartableEngineState]) {
      start()
    } else if (e.state.isInstanceOf[StoppedEngineState]) {
      done.countDown()
    }
  })

  /**
   * Start the task in the supplied execution context and return immediately
   */
  def start() {
    Future {
      task.start()
    }
  }

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