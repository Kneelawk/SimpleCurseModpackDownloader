package org.kneelawk.simplecursemodpackdownloader.task

trait TaskBuilder {
  /** Used for setting the task's parent
   *  
   * This is important for hierarchical tasks.
   */
  def setParent(parent: Task)
  
  /** Builds the task
   *  
   */
  def build(): Task
}