package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.util.concurrent.ExecutionException

import scala.util.Failure
import scala.util.Success

import org.apache.http.ConnectionClosedException
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.nio.client.HttpAsyncClient
import org.kneelawk.simplecursemodpackdownloader.curse.CurseUtils
import org.kneelawk.simplecursemodpackdownloader.curse.NoFileForMinecraftVersionException
import org.kneelawk.simplecursemodpackdownloader.io.InvalidModFileException
import org.kneelawk.simplecursemodpackdownloader.io.ZipUtils
import org.kneelawk.simplecursemodpackdownloader.net.RedirectUrlSanitizer
import org.kneelawk.simplecursemodpackdownloader.net.StatusCodeException
import org.kneelawk.simplecursemodpackdownloader.net.URIUtil
import org.kneelawk.simplecursemodpackdownloader.task.EngineState

import dispatch.Defaults.executor
import dispatch.Http
import dispatch.StatusCode

/*
 * This file is where the magic happens.
 */

class ModpackEngine(client: Http, authToken: String, modpack: ModpackManifest,
    modsDir: File, listener: ModpackProgressListener) {
  import EngineState._

  def start {
    // This is really hacked in
    val downloadClient = HttpAsyncClients.custom()
      .setRedirectStrategy(new RedirectUrlSanitizer).build()
    downloadClient.start()

    /*
     * Note: This system doesn't regulate the number of concurrent downloads.
     * This system will likely need to be modified.
     */
    def engineLoop: EngineState = {
      val numMods = modpack.files.size
      var engines = modpack.files.map(f =>
        new ModEngine(client, downloadClient, authToken, modpack.minecraft.version, f.projectId,
          f.fileId, modsDir, listener.createModProgressListener(f.projectId, f.fileId)))
      engines.foreach(_.start)

      var numEngines = engines.size
      while (engines.size > 0) {
        engines = engines.filter(e => e.state != Finished)
        engines.filter(_.state == Crashed).foreach(_.start)
        if (engines.count(_.state == Aborted) > 0) {
          return Aborted
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
    if (result == Aborted) {
      listener.onAbort
    } else if (result == Dead) {
      listener.onDeath
    } else {
      listener.onAllModsComplete
    }

    downloadClient.close()
  }
}

/**
 * This locates and downloads a file based on project and file ids.
 */
class ModEngine(client: Http, downloadClient: HttpAsyncClient, authToken: String, minecraftVersion: String,
    projectId: Int, fileId: Int, modsDir: File, listener: ModProgressListener) {
  import EngineState._

  @volatile var state: EngineState = NotStarted

  var error: Throwable = null

  def start {
    if (state != Running) {
      state = Running
      listener.onBeginResolvingMod
      val futMod = CurseUtils.getExistingAddonFile(client, authToken, projectId, fileId, minecraftVersion)
      futMod.onComplete {
        case Success(mod) => {
          listener.onModResolved(mod)

          val sanitaryUri = URIUtil.sanitizeCurseDownloadUri(mod.downloadUrl, true)

          val outFileName = mod.downloadUrl.replaceAll("^.*\\/", "")
          val outFile = new File(modsDir, outFileName)
          val download = new Download(outFile, sanitaryUri.toASCIIString())
            .onDownloadStarted(d => listener.onBeginModDownload)
            .onDownloadProgress(p => listener.onModDownloadProgress(p.downloaded, p.maxSize))
            .onDownloadError(err => {
              err match {
                case StatusCodeException(_, _) => {
                  error = err
                  state = Dead
                  listener.onDeath(err)
                }
                case _: ConnectionClosedException => {
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
            })
            .onDownloadComplete(c => {
              val integrityCheck = if (outFileName.endsWith(".jar"))
                ZipUtils.checkJarIntegrity(outFile)
              else ZipUtils.checkZipIntegrity(outFile)
              if (integrityCheck) {
                state = Finished
                listener.onCompletedModDownload(c.size)
              } else {
                state = Crashed
                listener.onCrash(new InvalidModFileException(s"Invalid mod download: $outFileName"))
                outFile.delete()
              }
            })
            .start(downloadClient)
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