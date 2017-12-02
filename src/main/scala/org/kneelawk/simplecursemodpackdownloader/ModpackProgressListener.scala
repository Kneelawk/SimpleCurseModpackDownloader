package org.kneelawk.simplecursemodpackdownloader

import org.kneelawk.simplecursemodpackdownloader.curse.ModFile

/*
 * Structure thoughts:
 * Single ProgressListener is for all files?
 * 
 * How big a deal will zombie processes be?
 * 
 * Stages:
 * overall progress update,
 * beginning mod resolution,
 * mod name found,
 * beginning mod download,
 * mod download progress update,
 * completed mod download,
 * completed all mod downloads,
 * error/abort,
 * Potential zombie management stages:
 * zombie detected,
 * killing zombie,
 * restarting killed zombie
 */
trait ModpackProgressListener {
  def onOverallProgress(current: Long, max: Long)

  def createModProgressListener(projectId: Int, fileId: Int): ModProgressListener

  def onAllModsComplete

  def onDeath

  def onAbort
}

/*
 * Error terminology:
 * Error - means that an exception was thrown but it didn't stop the process.
 * Crash - means that the process was stopped and needs to be reinitialized.
 * Death - means that a fatal error has occurred and anything relying on this process cannot continue.
 * Abort - means that this process was canceled by the user. Anything depending on this process cannot continue.
 */

/*
 * A listener for each file?
 * Question: Should there be an event for a mod with an invalid fileId defaulting?
 */
trait ModProgressListener {
  def onBeginResolvingMod

  def onModResolved(mod: ModFile)

  def onBeginModDownload

  def onModDownloadProgress(current: Long, max: Long)

  def onCompletedModDownload(size: Long)

  def onError(t: Throwable)

  def onCrash(t: Throwable)

  def onDeath(t: Throwable)

  def onAbort
}