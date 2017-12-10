package org.kneelawk.simplecursemodpackdownloader.task

/*
 * Error terminology:
 * Error - means that an exception was thrown but it didn't stop the process.
 * Crash - means that the process was stopped and needs to be reinitialized.
 * Death - means that a fatal error has occurred and anything relying on this process cannot continue.
 */

object EngineState {
  object NotStarted extends EngineState
  object Running extends EngineState
  object Finished extends EngineState with StoppedEngineState
  object Crashed extends EngineState with RestartableEngineState
  object Dead extends EngineState with StoppedEngineState
  object Abort extends EngineState with StoppedEngineState
  object ZombieKill extends EngineState with RestartableEngineState
}

sealed trait EngineState

sealed trait RestartableEngineState extends EngineState
sealed trait StoppedEngineState extends EngineState