package org.kneelawk.simplecursemodpackdownloader.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.LinkOption

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

  def copyFile(from: File, to: File, createParents: Boolean) {
    if (createParents && !to.getParentFile.exists())
      to.getParentFile.mkdirs()
    Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS)
  }

  def copyDir(from: File, to: File) {
    val fromPath = from.toPath()
    val toPath = to.toPath()
    Files.walk(fromPath)
      .forEach((p: Path) => {
        val toIndividual = toPath.resolve(fromPath.relativize(p))
        if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
          if (!Files.exists(toIndividual))
            Files.createDirectories(toIndividual)
        } else {
          if (!Files.exists(toIndividual.getParent))
            Files.createDirectories(toIndividual.getParent)
          Files.copy(p, toIndividual, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS)
        }
      })
  }
}