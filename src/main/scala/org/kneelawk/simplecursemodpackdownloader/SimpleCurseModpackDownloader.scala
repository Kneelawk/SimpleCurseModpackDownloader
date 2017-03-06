package org.kneelawk.simplecursemodpackdownloader

import org.kneelawk.simplecursemodpackdownloader.cli.CLIDownloader
import org.kneelawk.simplecursemodpackdownloader.gui.GUIDownloader

object SimpleCurseModpackDownloader {
  def main(args: Array[String]) {
    if (args.length > 1) {
      args(0).toLowerCase() match {
        case "cli" => {
          CLIDownloader(args)
        }
        case "simple" => {
          SimpleDownloader(args)
        }
        case _ => {
          GUIDownloader(args)
        }
      }
    } else {
      GUIDownloader(args)
    }
  }
}