package org.kneelawk.simplecursemodpackdownloader

/*
 * Tasks be hierarchical.
 * Task factories create tasks of different types.
 */

/** A trait of common methods for all tasks.
 *  
 */
trait Task {
  /** Sends an interrupt signal to this engine.
   *  
   *  For hierarchical tasks, this interrupt will percolate down through this task's children.
   */
  def interrupt(state: InterruptState)
  
  /** Adds a child task as a component of this task.
   *  
   */
  def addChild(child: Task)

  /** Gets the state of this engine.
   *  
   */
  def getState: EngineState
  
  /** Gets the last time this engine reported an update
   *  
   *  This is for zombie identification
   */
  def getLastUpdateTime: Long
  
  /** Starts the task once everything is set up.
   *  
   */
  def startTask
}