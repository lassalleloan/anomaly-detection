package com.loanlassalle.intelligentwaf

import java.net.URL

import scala.collection.mutable.ListBuffer
import scala.io.Source

class RawHttpRequest(val requestLine: String,
                     val requestHeaders: Map[String, String],
                     val messageBody: String) {

  private val requestLineSplit: Array[String] = requestLine.split(" ")

  val method: String = requestLineSplit.head
  val url: URL = new URL(requestLineSplit.tail.head)
  val path: String = if (url.getPath == null) "" else url.getPath
  val query: String = if (url.getQuery == null) "" else url.getQuery
  val parameters: Map[String, List[String]] = parseQuery
  val standard: String = requestLineSplit.last

  override def toString: String =
    s"$requestLine\n" +
      s"${requestHeaders.mkString("\n")}\n" +
      s"\n${if (messageBody.length > 0) messageBody else ""}"

  def segmentCount: Int = path.count(_ == '/')

  def isFile: Int = path.substring(path.lastIndexOf('/') + 1).contains('.').compareTo(false)

  def fileExtension: String = path.substring(path.lastIndexOf('.') + 1)

  def parameterCount: Int = query.count(_ == '=')

  def isStandardHeader(header: String): Int = RawHttpRequest.StandardHeaders.contains(header).compareTo(false)

  def isHeaderPresent(header: String): Int = requestHeaders.contains(header).compareTo(false)

  def isPersistentConnection: Int = (requestHeaders.getOrElse("Connection", "") == "keep-alive").compareTo(false)

  def nonStandardHeaderRatio: Double =
    if (requestHeaders.isEmpty)
      0
    else
      1 - standardHeaderRatio

  def standardHeaderRatio: Double =
    if (requestHeaders.isEmpty)
      0
    else
      requestHeaders.size / RawHttpRequest.StandardHeaders.size.toDouble

  def isBody: Int = messageBody.nonEmpty.compare(false)

  def lineCount(str: String): Int = str.split("\r\n|\r|\n").length

  def wordCount(str: String): Int = str.split("\\w+").length

  private def nonPrintableCharRatio(str: String): Double =
    if (str.isEmpty)
      str.length
    else
      1 - printableCharRatio(str)

  private def symbolRatio(str: String): Double =
    if (str.isEmpty)
      str.length
    else
      (printableCharRatio(str) - letterRatio(str) - digitRatio(str)) * str.length

  private def printableCharRatio(str: String): Double =
    if (str.isEmpty)
      str.length
    else
      "[ -~]".r.findAllIn(str).length / str.length.toDouble

  private def letterRatio(str: String): Double =
    if (str.isEmpty)
      str.length
    else
      str.count(_.isLetter) / str.length.toDouble

  private def digitRatio(str: String): Double =
    if (str.isEmpty)
      str.length
    else
      str.count(_.isDigit) / str.length.toDouble

  private def parseQuery: Map[String, List[String]] = {
    val separator = '&'

    if (query.isEmpty)
      Map[String, List[String]]()
    else
      query.split(separator)
        .map(parseParameter)
        .groupBy(_._1)
        .map(t => t._1 -> t._2.map(_._2).toList)
  }

  private def parseParameter(parameter: String): (String, String) = {
    val separator = '='

    val index = parameter.indexOf(separator)
    val key = if (index > 0) parameter.substring(0, index) else parameter
    val value = if (index > 0 && parameter.length > index + 1) parameter.substring(index + 1) else ""

    key -> value
  }

}

object RawHttpRequest {

  val StandardHeaders = List("Accept", "Accept-Charset", "Accept-Datetime", "Accept-Encoding",
    "Accept-Language", "Authorization", "Cache-Control", "Connection",
    "Content-Length", "Content-MD5", "Content-Type", "Cookie",
    "Date", "Expect", "From", "Host",
    "If-Match", "If-Modified-Since", "If-None-Match", "If-Range",
    "If-Unmodified-Since", "Max-Forwards", "Origin", "Pragma",
    "Proxy-Authorization", "Range", "Referer", "TE",
    "User-Agent", "Upgrade", "Via", "Warning")

  /**
    * Parse raw HTTP requests contain in file
    *
    * @param filename name of file contains raw HTTP requests
    * @throws java.io.FileNotFoundException if an I/O error occurs reading the input stream
    * @throws NoSuchElementException        if HTTP Request is malformed
    */
  def parseRawRequestFile(filename: String): ListBuffer[RawHttpRequest] = {
    val iterator = Source.fromFile(filename).getLines()
    val httpRequests = ListBuffer[RawHttpRequest]()

    while (iterator.hasNext) {
      httpRequests += parseRawRequest(iterator)
    }

    httpRequests
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

  private def parseHeader(header: String): (String, String) = {
    val separator = ": "

    val index = header.indexOf(separator)
    if (index == -1)
      throw new NoSuchElementException("Invalid Header Parameter: " + header)

    header.substring(0, index) -> header.substring(index + separator.length, header.length())
  }

}
