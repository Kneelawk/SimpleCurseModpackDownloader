package org.kneelawk.simplecursemodpackdownloader.verysimple

import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadFailedEvent
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadTaskBuilder
import org.apache.http.client.methods.HttpGet
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadProgressEvent
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadCompleteEvent
import java.io.File
import org.kneelawk.simplecursemodpackdownloader.net.NetworkContext
import org.kneelawk.simplecursemodpackdownloader.net.FileDownloadStartedEvent

object VerySimpleDownloader {
  def apply(args: Array[String]) {
    val netCtx = NetworkContext()
    netCtx.start()

    new FileDownloadTaskBuilder(netCtx).setFile(new File("working-dir/output.jar"))
      .setRequest(new HttpGet("https://addons.cursecdn.com/files/2503/244/rftoolsctrl-1.12-1.7.0.jar")).getBus
      .register((e: FileDownloadStartedEvent) => println("Starting download..."))
      .register((e: FileDownloadProgressEvent) => println(e.current + " / " + e.max))
      .register((e: FileDownloadFailedEvent) => {
        e.exception.printStackTrace()
        netCtx.close()
      })
      .register((e: FileDownloadCompleteEvent) => {
        println("Done.")
        netCtx.close()
      })
      .builder.build().start()
  }
}