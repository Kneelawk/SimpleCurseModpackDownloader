package org.kneelawk.simplecursemodpackdownloader.net

import org.kneelawk.simplecursemodpackdownloader.InterruptState

/*
 * NetworkClient hierarchy.
 * 
 * How much functionality should be defined here?
 * 
 * Client types:
 * DownloadClient, RestClient?, StringClient?
 */
trait NetworkClient {
  def setParent(parent: NetworkClient)

  def interrupt(state: InterruptState)
}