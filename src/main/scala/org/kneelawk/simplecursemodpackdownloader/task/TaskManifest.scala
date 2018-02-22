package org.kneelawk.simplecursemodpackdownloader.task

import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import java.util.concurrent.locks.ReentrantLock
import org.kneelawk.simplecursemodpackdownloader.util.LockUtil._

class TaskManifest {
  private val tasks = new HashSet[Task]
  private val tasksLock = new ReentrantLock

  /** Add a task to the manifest.
   *  
   */
  def addTask(task: Task) = lock(tasksLock)(tasks += task)
  
  /** Add a task to the manifest.
   *  
   */
  def +=(task: Task) = lock(tasksLock)(tasks += task)

  /** Remove a task from the manifest.
   *  
   */
  def removeTask(task: Task) = lock(tasksLock)(tasks -= task)
  
  /** Remove a task from the manifest.
   *  
   */
  def -=(task: Task) = lock(tasksLock)(tasks -= task)

  /** Get a list of tasks in the manifest.
   *  
   */
  def getTasks: Set[Task] = tasks
  
  /** Get the lock in control of tasks.
   *  
   */
  def getLock: ReentrantLock = tasksLock

  /** Loop through every task in the manifest.
   *  
   */
  def foreach[R](f: (Task) => R) = toList.foreach(f)

  /** Remove all stopped tasks.
   *  
   */
  def pruneTasks() {
    for (elem <- toList) {
      if (!elem.isAllive) removeTask(elem)
    }
  }
  
  /** Converts this TaskManifest into an immutable list of tasks.
   *  
   */
  def toList: List[Task] = lock(tasksLock)(tasks.toList)
}