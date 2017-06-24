package org.kneelawk.simplecursemodpackdownloader.net

import org.kneelawk.simplecursemodpackdownloader.InterruptState

trait NetworkClient {
  def setParent(parent: NetworkClient)

  def interrupt(state: InterruptState)
}