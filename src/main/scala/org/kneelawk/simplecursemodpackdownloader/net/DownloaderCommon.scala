package org.kneelawk.simplecursemodpackdownloader.net

import java.io.File
import java.io.IOException

import scala.collection.mutable.MultiMap

import org.apache.http.client.methods.HttpUriRequest

/**
 * Thrown when a DownloadStartedEvent has its invalid response flag set.
 */
case class InvalidResponseException(req: HttpUriRequest, tpe: InvalidResponseType,
  statusCode: Int, statusText: String, headers: MultiMap[String, String]) extends IOException

/**
 * Kinds of invalid responses.
 */
trait InvalidResponseType
object InvalidResponseType {
  object None extends InvalidResponseType
  object Recoverable extends InvalidResponseType
  object Fatal extends InvalidResponseType
}