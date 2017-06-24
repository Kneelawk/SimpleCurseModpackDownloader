package org.kneelawk.simplecursemodpackdownloader.net

import java.net.URI

object URIUtil {
  val defaultCurseScheme = "https"
  val defaultCurseHost = "addons-origin.cursecdn.com"
  val defaultCursePath = "/files/"

  val fullUriPattern = "^([^\\/]+):\\/\\/([^\\/]+)(\\/.*)?$".r
  val hostlessUriPattern = "^([^\\/]+):\\/\\/(\\/.*)$".r
  val hostRelativeUriPattern = "^\\/\\/([^\\/]+)(\\/.*)?$".r
  val hostlessRelativeUriPattern = "^\\/\\/(\\/.*)$".r
  val pathRelativeUriPattern = "^(\\/.*)$".r

  def sanitizeCurseDownloadUri(insaneUri: String): URI =
    sanitizeUri(defaultCurseScheme, defaultCurseHost, defaultCursePath, insaneUri)

  def sanitizeUri(insaneUri: String): URI = sanitizeUri(null, null, null, insaneUri)

  def sanitizeUri(base: URI, insaneUri: String): URI =
    sanitizeUri(base.getScheme, base.getHost, base.getPath, insaneUri)

  def sanitizeUri(baseScheme: String, baseHost: String, basePath: String, insaneUri: String): URI = {
    insaneUri match {
      case fullUriPattern(scheme, host, path) => {
        new URI(scheme, host, path, null)
      }
      case hostlessUriPattern(scheme, path) => {
        new URI(scheme, null, path, null)
      }
      case hostRelativeUriPattern(host, path) => {
        new URI(baseScheme, host, path, null)
      }
      case hostlessRelativeUriPattern(path) => {
        new URI(baseScheme, null, path, null)
      }
      case pathRelativeUriPattern(path) => {
        new URI(baseScheme, baseHost, path, null)
      }
      case path => {
        new URI(baseScheme, baseHost,
          if (basePath != null)
            (if (basePath.endsWith("/")) basePath else basePath + "/") + path
          else path, null)
      }
    }
  }
}