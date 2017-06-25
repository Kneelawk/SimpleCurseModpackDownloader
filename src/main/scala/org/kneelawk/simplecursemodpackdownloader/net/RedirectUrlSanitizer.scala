package org.kneelawk.simplecursemodpackdownloader.net

import org.apache.http.impl.client.DefaultRedirectStrategy
import java.net.URI
import org.apache.http.client.utils.URIBuilder
import org.apache.http.ProtocolException
import java.net.URISyntaxException
import java.util.Locale
import org.apache.http.util.TextUtils

class RedirectUrlSanitizer extends DefaultRedirectStrategy {
  override def createLocationURI(location: String): URI = {
    try {
      val b = new URIBuilder(URIUtil.sanitizeUri(location, true).normalize())
      val host = b.getHost
      if (host != null) {
        b.setHost(host.toLowerCase(Locale.ROOT))
      }
      val path = b.getPath
      if (TextUtils.isEmpty(path)) {
        b.setPath("/")
      }
      val uri = b.build()
      // Potential degugging point?
      uri
    } catch {
      case ex: URISyntaxException => {
        throw new ProtocolException(s"Invalid redirect URI: $location", ex)
      }
    }
  }
}