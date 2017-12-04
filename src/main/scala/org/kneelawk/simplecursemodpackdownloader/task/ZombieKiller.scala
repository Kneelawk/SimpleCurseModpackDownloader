package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.ListBuffer

class ZombieKiller {
  private val tasks = new ListBuffer[Task]
  
  def addTask(task: Task) = tasks += task
}