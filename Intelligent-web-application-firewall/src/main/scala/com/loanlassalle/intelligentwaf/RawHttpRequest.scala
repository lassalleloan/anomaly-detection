package com.loanlassalle.intelligentwaf

import java.net.URL

import scala.collection.mutable.ListBuffer
import scala.io.Source

class RawHttpRequest(val requestLine: String,
                     val requestHeaders: Map[String, String],
                     val messageBody: String) {

  private val requestLineSplited = requestLine.split(" ")
  private val method = requestLineSplited.head
  private val url = new URL(requestLineSplited.tail.head)
  private val path = url.getPath
  private val query = if (url.getQuery == null) "" else url.getQuery
  private val standard = requestLineSplited.last

  def segmentCount: Long = path.count(_ == '/')
  def alphaNumRatio: Double = path.count(c => c.isLetterOrDigit || c == '/').toDouble / path.length
  def parameterCount: Long = query.count(_ == '=')
  def isUserAgentPresent: Boolean = requestHeaders.isDefinedAt("User-Agent")
  def isPersistentConnection: Boolean = requestHeaders.getOrElse("Connection", "") == "keep-alive"

  override def toString: String =
    s"$method," +
      s"$path,$segmentCount,${path.length},${"%.2f".format(alphaNumRatio)}," +
      s"$query,${query.length},$parameterCount,$standard," +
      s"${requestHeaders.mkString(",")},${requestHeaders.size},$isUserAgentPresent,$isPersistentConnection," +
      s"$messageBody,${messageBody.length}"

}

object RawHttpRequest {

  def parseHeader(header: String): (String, String) = {
    val separator = ": "
    val index = header.indexOf(separator)
    if (index == -1)
      throw new NoSuchElementException("Invalid Header Parameter: " + header)

    (header.substring(0, index), header.substring(index + separator.length, header.length()))
  }

  /**
    * Parse an raw HTTP request
    *
    * @param iterator iterator on strings holding raw HTTP request
    * @throws NoSuchElementException if HTTP Request is malformed
    */
  def parseRawRequest(iterator: Iterator[String]): RawHttpRequest = {

    // Request-Line              ; Section 5.1
    val requestLine = iterator.next()

    // *(( general-header        ; Section 4.5
    //  | request-header         ; Section 5.3
    //  | entity-header ) CRLF)  ; Section 7.1
    // CRLF
    val requestHeaders = iterator.takeWhile(_.length > 0).map(parseHeader).toMap

    // [ message-body ]          ; Section 4.3
    val messageBody = iterator.takeWhile(_.length > 0).mkString("\n")

    new RawHttpRequest(requestLine, requestHeaders, messageBody)
  }

  /**
    * Parse raw HTTP requests contain in file
    *
    * @param filename name of file contains raw HTTP requests
    * @throws java.io.FileNotFoundException if an I/O error occurs reading the input stream
    * @throws NoSuchElementException if HTTP Request is malformed
    */
  def parseRawRequestFile(filename: String): ListBuffer[RawHttpRequest] = {
    val iterator = Source.fromFile(filename).getLines()
    val httpRequests = ListBuffer[RawHttpRequest]()

    while (iterator.hasNext) {
      httpRequests += parseRawRequest(iterator)
    }

    httpRequests
  }

}
