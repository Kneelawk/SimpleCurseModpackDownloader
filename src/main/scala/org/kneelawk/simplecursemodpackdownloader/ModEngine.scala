package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.util.concurrent.ExecutionException

import scala.util.Failure
import scala.util.Success

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.StatusCode
import java.net.URL
import java.net.URI

/*
 * This file is where the magic happens.
 */

class ModpackEngine(client: Http, authToken: String, modpack: ModpackManifest,
    modsDir: File, listener: ModpackProgressListener) {
  import EngineState._

  def start {
    /*
     * Note: This system doesn't regulate the number of concurrent downloads.
     * This system will likely need to be modified.
     */
    def engineLoop: EngineState.Value = {
      val numMods = modpack.files.size
      var engines = modpack.files.map(f =>
        new ModEngine(client, authToken, modpack.minecraft.version, f.projectId,
          f.fileId, modsDir, listener.createModProgressListener(f.projectId, f.fileId)))
      engines.foreach(_.start)

      var numEngines = engines.size
      while (engines.size > 0) {
        engines = engines.filter(e => e.state != Finished)
        engines.filter(_.state == Crashed).foreach(_.start)
        if (engines.count(_.state == Abort) > 0) {
          return Abort
        }
        if (engines.count(_.state == Dead) > 0) {
          return Dead
        }
        if (engines.size < numEngines && engines.size != 0) {
          numEngines = engines.size
          listener.onOverallProgress(numMods - numEngines, numMods)
        }
        Thread.sleep(200)
      }
      return Finished
    }

    val result = engineLoop
    if (result == Abort) {
      listener.onAbort
    } else if (result == Dead) {
      listener.onDeath
    } else {
      listener.onAllModsComplete
    }
  }
}

/**
 * This locates and downloads a file based on project and file ids.
 */
class ModEngine(client: Http, authToken: String, minecraftVersion: String,
    projectId: Int, fileId: Int, modsDir: File, listener: ModProgressListener) {
  import EngineState._

  @volatile var state = NotStarted

  var error: Throwable = null

  def start {
    if (state != Running) {
      state = Running
      listener.onBeginResolvingMod
      val futMod = CurseUtils.getExistingAddonFile(client, authToken, projectId, fileId, minecraftVersion)
      futMod.onComplete {
        case Success(mod) => {
          listener.onModResolved(mod)
          
          // copied from https://stackoverflow.com/a/8962869/1687581
          val url = new URL(mod.downloadUrl)
          val sanitaryUri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getRef());

          val outFile = new File(modsDir, mod.downloadUrl.replaceAll("^.*\\/", ""))
          val download = new Download(outFile, sanitaryUri.toASCIIString())
            .onDownloadStarted(d => listener.onBeginModDownload)
            .onDownloadProgress(p => listener.onModDownloadProgress(p.downloaded, p.maxSize))
            .onDownloadError(err => listener.onError(err))
            .start(client)

          download.onComplete {
            case Success(f) => {
              state = Finished
              listener.onCompletedModDownload
            }
            case Failure(err) => {
              err match {
                case t: ExecutionException if t.getCause.isInstanceOf[StatusCode] => {
                  error = err
                  state = Dead
                  listener.onDeath(err)
                }
                case StatusCode(_) => {
                  error = err
                  state = Dead
                  listener.onDeath(err)
                }
                case _ => {
                  error = err
                  state = Crashed
                  listener.onCrash(err)
                }
              }
            }
          }
        }
        case Failure(err) => {
          err match {
            case NoFileForMinecraftVersionException(_) => {
              error = err
              state = Dead
              listener.onDeath(err)
            }
            case t: ExecutionException if t.getCause.isInstanceOf[StatusCode] => {
              error = err
              state = Dead
              listener.onDeath(err)
            }
            case StatusCode(_) => {
              error = err
              state = Dead
              listener.onDeath(err)
            }
            case _ => {
              error = err
              state = Crashed
              listener.onCrash(err)
            }
          }
        }
      }
    }
  }
}