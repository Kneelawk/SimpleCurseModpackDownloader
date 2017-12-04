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
  object Finished extends EngineState
  object Crashed extends EngineState with RestartableEngineState
  object Dead extends EngineState
  object Abort extends EngineState
  object ZombieKill extends EngineState with RestartableEngineState
}

sealed trait EngineState

sealed trait RestartableEngineState extends EngineState