package org.kneelawk.simplecursemodpackdownloader.task

trait TaskBuilder {
  /** Get this tasks event bus.
   *  
   *  Used for registering listeners before the task has started.
   */
  def getBus: TaskEventBus[this.type]
  
  /** Builds the task
   *  
   */
  def build(): Task
}