package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration

class ZombieKiller(manifest: TaskManifest, period: Duration, zombieTaskWait: Duration) {
  import ZombieKiller.killZombies
  
  private var running = false
  
  /**
   * Starts the zombie killer
   */
  def start() {
    val t = new Thread(() => loop())
    t.start()
  }
  
  /**
   * Stops the zombie killer
   */
  def stop() {
    running = false
  }
  
  private def loop() {
    running = true
    while (running) {
      // only prune every 30 seconds
      Thread.sleep(period.toMillis)
      
      killZombies(manifest, zombieTaskWait)
    }
  }
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