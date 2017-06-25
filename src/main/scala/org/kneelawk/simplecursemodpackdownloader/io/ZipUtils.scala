package org.kneelawk.simplecursemodpackdownloader.io

import java.nio.file.Files
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException

object ZipUtils {
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

  def checkZipIntegrity(zip: File): Boolean = {
    try {
      var hasFiles = false
      val zis = new ZipInputStream(new FileInputStream(zip))
      Stream.continually(zis.getNextEntry).takeWhile(_ != null).foreach(e => {
        hasFiles = true
        zis.closeEntry()
      })
      zis.close()
      hasFiles
    } catch {
      case _: IOException => false
    }
  }

  def checkJarIntegrity(zip: File): Boolean = {
    try {
      var hasManifest = false
      val zis = new ZipInputStream(new FileInputStream(zip))
      Stream.continually(zis.getNextEntry).takeWhile(_ != null).foreach(e => {
        if (e.getName == "META-INF/MANIFEST.MF")
          hasManifest = true
        zis.closeEntry()
      })
      zis.close()
      hasManifest
    } catch {
      case _: IOException => false
    }
  }
}