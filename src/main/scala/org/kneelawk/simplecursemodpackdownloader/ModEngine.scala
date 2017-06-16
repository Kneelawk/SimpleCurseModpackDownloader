package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.util.concurrent.ExecutionException

import scala.util.Failure
import scala.util.Success

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.StatusCode

/*
 * This file is where the magic happens.
 */

class ModpackEngine(client: Http, authToken: String, modpack: ModpackManifest,
    modsDir: File, listener: ModpackProgressListener) {
  import ModEngineState._

  def start {
    /*
     * Note: This system doesn't regulate the number of concurrent downloads.
     * This system will likely need to be modified.
     */
    var engines = modpack.files.map(f =>
      new ModEngine(client, authToken, modpack.minecraft.version, f.projectId,
        f.fileId, modsDir, listener.createModProgressListener(f.projectId, f.fileId)))
    engines.foreach(_.start)

    while (engines.size > 0) {
      engines = engines.filter(e => e.state == Running || e.state == Crashed)
      engines.filter(_.state == Crashed).foreach(_.start)
    }
  }
}

/**
 * This locates and downloads a file based on project and file ids.
 */
class ModEngine(client: Http, authToken: String, minecraftVersion: String,
    projectId: Int, fileId: Int, modsDir: File, listener: ModProgressListener) {
  import ModEngineState._

  @volatile var state = NotStarted

  var error: Throwable = null

  def start {
    state = Running
    if (state != Running) {
      listener.onBeginResolvingMod
      val futMod = CurseUtils.getExistingAddonFile(client, authToken, projectId, fileId, minecraftVersion)
      futMod.onComplete {
        case Success(mod) => {
          listener.onModResolved(mod)

          val outFile = new File(modsDir, mod.downloadUrl.replaceAll("^.*\\/", ""))
          val download = new Download(outFile, mod.downloadUrl)
            .onDownloadStarted(d => listener.onBeginModDownload)
            .onDownloadProgress(p => listener.onModDownloadProgress(p.downloaded, p.maxSize))
            .onDownloadError(err => listener.onError(err))
            .start(client)

          download.onComplete {
            case Success(f) => {
              listener.onCompletedModDownload
              state = Finished
            }
            case Failure(err) => {
              err match {
                case t: ExecutionException if t.getCause.isInstanceOf[StatusCode] => {
                  listener.onDeath(err)
                  error = err
                  state = Dead
                }
                case StatusCode(_) => {
                  listener.onDeath(err)
                  error = err
                  state = Dead
                }
                case _ => {
                  listener.onCrash(err)
                  error = err
                  state = Crashed
                }
              }
            }
          }
        }
        case Failure(err) => {
          err match {
            case NoFileForMinecraftVersionException(_) => {
              listener.onDeath(err)
              error = err
              state = Dead
            }
            case t: ExecutionException if t.getCause.isInstanceOf[StatusCode] => {
              listener.onDeath(err)
              error = err
              state = Dead
            }
            case StatusCode(_) => {
              listener.onDeath(err)
              error = err
              state = Dead
            }
            case _ => {
              listener.onCrash(err)
              error = err
              state = Crashed
            }
          }
        }
      }
    }
  }
}

/*
 * Error terminology:
 * Error - means that an exception was thrown but it didn't stop the process.
 * Crash - means that the process was stopped and needs to be reinitialized.
 * Death - means that a fatal error has occurred and anything relying on this process cannot continue.
 */

object ModEngineState extends Enumeration {
  val NotStarted, Running, Finished, Crashed, Dead, Abort = Value
}