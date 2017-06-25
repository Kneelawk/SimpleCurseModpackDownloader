package org.kneelawk.simplecursemodpackdownloader.net

import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.Charset

import org.apache.commons.codec.DecoderException
import org.apache.commons.codec.binary.StringUtils

object URIUtil {
  val defaultCurseScheme = "https"
  val defaultCurseHost = "addons-origin.cursecdn.com"
  val defaultCursePath = "/files/"

  val fullUriPattern = "^([^\\/]+):\\/\\/([^\\/]+)(\\/.*)?$".r
  val hostlessUriPattern = "^([^\\/]+):\\/\\/(\\/.*)$".r
  val hostRelativeUriPattern = "^\\/\\/([^\\/]+)(\\/.*)?$".r
  val hostlessRelativeUriPattern = "^\\/\\/(\\/.*)$".r
  val pathRelativeUriPattern = "^(\\/.*)$".r

  def sanitizeCurseDownloadUri(insaneUri: String, unescapePath: Boolean): URI =
    sanitizeUri(defaultCurseScheme, defaultCurseHost, defaultCursePath, insaneUri, unescapePath)

  def sanitizeUri(insaneUri: String, unescapePath: Boolean): URI =
    sanitizeUri(null, null, null, insaneUri, unescapePath)

  def sanitizeUri(base: URI, insaneUri: String, unescapePath: Boolean): URI =
    sanitizeUri(base.getScheme, base.getHost, base.getPath, insaneUri, unescapePath)

  def sanitizeUri(baseScheme: String, baseHost: String, basePath: String,
    insaneUri: String, unescapePath: Boolean): URI = {
    insaneUri match {
      case fullUriPattern(scheme, host, path) => {
        new URI(scheme, host, if (unescapePath) unescape(path) else path, null)
      }
      case hostlessUriPattern(scheme, path) => {
        new URI(scheme, null, if (unescapePath) unescape(path) else path, null)
      }
      case hostRelativeUriPattern(host, path) => {
        new URI(baseScheme, host, if (unescapePath) unescape(path) else path, null)
      }
      case hostlessRelativeUriPattern(path) => {
        new URI(baseScheme, null, if (unescapePath) unescape(path) else path, null)
      }
      case pathRelativeUriPattern(path) => {
        new URI(baseScheme, baseHost, if (unescapePath) unescape(path) else path, null)
      }
      case path => {
        new URI(baseScheme, baseHost,
          if (basePath != null)
            (if (basePath.endsWith("/")) basePath else basePath + "/")
            + (if (unescapePath) unescape(path) else path)
          else (if (unescapePath) unescape(path) else path), null)
      }
    }
  }

  def unescape(s: String): String = {
    val bytes = StringUtils.getBytesUsAscii(s)
    val buffer = new ByteArrayOutputStream()
    var i = 0
    while (i < bytes.length) {
      val b = bytes(i)
      if (b == '%') {
        try {
          val u = Character.digit(bytes(i + 1), 16)
          val l = Character.digit(bytes(i + 2), 16)
          i += 2
          buffer.write(((u & 0xF) << 4) | (l & 0xF))
        } catch {
          case e: ArrayIndexOutOfBoundsException => new DecoderException("Invalid URL encoding: ", e)
        }
      } else {
        buffer.write(b)
      }
      i += 1
    }
    new String(buffer.toByteArray(), Charset.defaultCharset())
  }
}