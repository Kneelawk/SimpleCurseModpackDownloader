package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set

class TaskManifest {
  private val tasks = new HashSet[Task]

  /** Add a task to the manifest.
   *  
   */
  def addTask(task: Task) = tasks += task
  
  /** Add a task to the manifest.
   *  
   */
  def +=(task: Task) = tasks += task

  /** Remove a task from the manifest.
   *  
   */
  def removeTask(task: Task) = tasks -= task
  
  /** Remove a task from the manifest.
   *  
   */
  def -=(task: Task) = tasks -= task

  /** Get a list of tasks in the manifest.
   *  
   */
  def getTasks: Set[Task] = tasks

  /** Loop through every task in the manifest.
   *  
   */
  def foreach[R](f: (Task) => R) = tasks.foreach(f)

  /** Remove all stopped tasks.
   *  
   */
  def pruneTasks() {
    tasks.retain(_.isAllive)
  }
  
  /** Converts this TaskManifest into an immutable list of tasks.
   *  
   */
  def toList: List[Task] = tasks.toList
}