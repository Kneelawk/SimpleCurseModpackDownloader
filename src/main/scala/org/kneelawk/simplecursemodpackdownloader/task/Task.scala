package org.kneelawk.simplecursemodpackdownloader.task

import org.kneelawk.simplecursemodpackdownloader.event.EventBus

/*
 * Tasks be hierarchical.
 * Task factories create tasks of different types.
 * 
 * Should there be TaskBuilders and TaskContexts?
 * A TaskBuilder would be responsible for initializing a task, taking settings, and notifying parent tasks.
 * Does that mean that the TaskBuilder and the EventBus would be integratable?
 * A TaskContext would be responsible for building the TaskBuilder and supplying it with persistent
 * context variables (things like an apache HttpClient)
 * 
 * No, we need to have unified TaskContexts for each aspect of the context.
 * So an HttpContext would house the apache HttpClient and builders would just require appropriate contexts
 * in their constructors.
 * Do I need a trait for TaskContexts? Or should I just have various TaskBuilders require various Contexts?
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
  
  /** Gets this task's event bus.
   *  
   */
  def getBus: EventBus

  /** Gets the state of this engine.
   *  
   */
  def getState: EngineState
  
  /** Is this task still allive/restartable?
   *  
   */
  def isAllive: Boolean = !getState.isInstanceOf[StoppedEngineState]
  
  /** Gets the last time this engine reported an update
   *  
   *  This is for zombie identification
   */
  def getLastUpdateTime: Long
  
  /** Starts the task once everything is set up.
   *  
   */
  def start()
}