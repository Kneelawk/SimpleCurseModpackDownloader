package org.kneelawk.simplecursemodpackdownloader

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
import java.nio.file.Path

object FileUtils {
  def unzipZip(zip: File): File = {
    val tempDir = Files.createTempDirectory("cursemodpackdownloader").toFile()
    tempDir.deleteOnExit()
    val zis = new ZipInputStream(new FileInputStream(zip))
    Stream.continually(zis.getNextEntry).takeWhile(_ != null).foreach { entry =>
      val outFile = new File(tempDir, entry.getName)
      outFile.deleteOnExit()
      if (entry.isDirectory()) {
        outFile.mkdirs()
      } else {
        val fos = new FileOutputStream(outFile)
        val data = new Array[Byte](4096)
        Stream.continually(zis.read(data)).takeWhile(_ >= 0).foreach(fos.write(data, 0, _))
        fos.close()
      }
      zis.closeEntry()
    }
    zis.close()
    return tempDir
  }

  def copyFile(from: File, to: File) {
    Files.copy(from.toPath(), to.toPath())
  }

  def copyDir(from: File, to: File) {
    val fromPath = from.toPath()
    val toPath = to.toPath()
    Files.walk(fromPath).filter((p: Path) => Files.isDirectory(p))
      .forEach((p: Path) => Files.copy(p, toPath.resolve(fromPath.relativize(p))))
  }
}