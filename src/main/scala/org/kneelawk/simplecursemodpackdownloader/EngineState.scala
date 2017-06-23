package org.kneelawk.simplecursemodpackdownloader

/*
 * Error terminology:
 * Error - means that an exception was thrown but it didn't stop the process.
 * Crash - means that the process was stopped and needs to be reinitialized.
 * Death - means that a fatal error has occurred and anything relying on this process cannot continue.
 */

object EngineState extends Enumeration {
  val NotStarted, Running, Finished, Crashed, Dead, Abort, ZombieKill = Value
}