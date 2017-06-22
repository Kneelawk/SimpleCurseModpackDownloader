package org.kneelawk.simplecursemodpackdownloader.net

trait NetworkClient {
  def createChild: NetworkClient
  
  // Should I have a separate event for zombie kill or should I
  // use some boolean value or abort type enum or something?
  def abort
}