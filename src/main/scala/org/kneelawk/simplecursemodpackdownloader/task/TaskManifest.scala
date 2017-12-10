package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

class TaskManifest {
  private val tasks = new HashSet[Task]

  def addTask(task: Task) = tasks += task

  def removeTask(task: Task) = tasks -= task

  def getTasks: Set[Task] = tasks

  def foreach[R](f: (Task) => R) = tasks.foreach(f)

  def pruneTasks() {
    tasks.retain((t) => t.getState.isInstanceOf[StoppedEngineState])
  }
}