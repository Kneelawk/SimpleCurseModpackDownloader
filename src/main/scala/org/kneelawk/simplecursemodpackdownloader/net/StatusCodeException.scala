package org.kneelawk.simplecursemodpackdownloader.net

import java.io.IOException

case class StatusCodeException(statusCode: Int, message: String) extends IOException(message)