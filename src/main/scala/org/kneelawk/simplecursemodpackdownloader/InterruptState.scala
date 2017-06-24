package org.kneelawk.simplecursemodpackdownloader

object InterruptState {
//  val Abort, ZombieKill = Value
  object Abort extends InterruptState
  object ZombieKill extends InterruptState
}

sealed trait InterruptState