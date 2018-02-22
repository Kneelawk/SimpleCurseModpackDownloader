package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration

class ZombieKiller {
  private val tasks = new ListBuffer[Task]

  def addTask(task: Task) = tasks += task
}

object ZombieKiller {
  def killZombies(manifest: TaskManifest, zombieTaskWait: Duration) {
    manifest.pruneTasks()
    val currentTime = System.currentTimeMillis()
    manifest.foreach(t => {
      if (currentTime - t.getLastUpdateTime > zombieTaskWait.toMillis
        && !t.isBlocked && !t.getState.isInstanceOf[HaltingEngineState]) {
        t.interrupt(InterruptState.ZombieKill)
      }
    })
  }
}