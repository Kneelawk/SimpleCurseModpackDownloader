package org.kneelawk.simplecursemodpackdownloader.task

import scala.concurrent.ExecutionContext
import java.util.concurrent.CountDownLatch
import scala.concurrent.duration.Duration
import scala.concurrent.Future

class TaskHandler(task: Task)(implicit ctx: ExecutionContext) {
  private val done = new CountDownLatch(1)
  
  task.getBus.register((e: TaskStateChangeEvent) => {
    if (e.state.isInstanceOf[RestartableEngineState]) {
      start()
    } else if (e.state.isInstanceOf[StoppedEngineState]) {
      done.countDown()
    }
  })

  def start() {
    Future {
      task.start()
    }
  }

  def await() {
    done.await()
  }

  def await(d: Duration) {
    done.await(d.length, d.unit)
  }
}