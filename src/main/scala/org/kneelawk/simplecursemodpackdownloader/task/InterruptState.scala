package org.kneelawk.simplecursemodpackdownloader.task

object InterruptState {
  object None extends InterruptState
  object Abort extends InterruptState
  object ZombieKill extends InterruptState
}

/** An enum for different reasons to interrupt a task.
 *  
 *  There are currently two kinds of interrupt states:<br>
 *  Abort - a signal that the user has requested the cancelation of a task.<br>
 *  ZombieKill - a signal that a zombie killer has found this task to be unresponsive and is attempting to restart it.
 */
sealed trait InterruptState