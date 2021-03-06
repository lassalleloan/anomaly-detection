package com.loanlassalle.intelligentwaf

import java.net.URL

import com.loanlassalle.intelligentwaf.RawHttpRequest._
import com.loanlassalle.intelligentwaf.util.Utils

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Raw HTTP request, RFC 2616
  *
  * @param id             identifier
  * @param requestLine    RFC 2616, Request-Line              ; Section 5.1
  * @param requestHeaders RFC 2616, *(( general-header        ; Section 4.5
  *                                  | request-header         ; Section 5.3
  *                                  | entity-header ) CRLF)  ; Section 7.1
  *                                  CRLF
  * @param messageBody    RFC 2616,  [ message-body ]         ; Section 4.3
  * @param label          type of raw HTTP request (e.g. normal)
  */
class RawHttpRequest(val id: BigInt,
                     requestLine: String,
                     requestHeaders: Seq[Header],
                     messageBody: String,
                     val label: String = "") {
  require(requestLine != null && requestLine.count(_.equals(' ')).equals(2),
    "requestLine must be not null and must contain two spaces")
  require(requestHeaders != null, "requestHeaders must be not null")
  require(messageBody != null, "messageBody must be not null")

  private val requestLineSplit: Array[String] = requestLine.split(" ")
  private val url: URL = try {
    new URL(requestLineSplit.tail.head)
  } catch {
    case _: Throwable => null
  }
  private val query: String = if (url == null || url.getQuery == null) "" else url.getQuery
  private val pathValue: String = if (url == null || url.getPath == null) "" else url.getPath

  val method: String = requestLineSplit.head
  val path: Path = Path(pathValue)
  val parameters: Seq[Parameter] = parseQuery(query)
  val standard: String = requestLineSplit.last
  val headers: Seq[Header] = requestHeaders
  val body: Body = new Body(messageBody)

  /**
    * Represents RawHttpRequest in CSV format with additional attributes
    *
    * @return RawHttpRequest in CSV format
    */
  def toCsv: String = f"$id," +
    f"$method," +
    f"${path.toCsv}," +
    f"${parameters.size}," +
    f"${query.length}," +
    f"${Utils.printableCharRatio(Seq(query))}%.2f," +
    f"${Utils.nonPrintableCharRatio(Seq(query))}%.2f," +
    f"${Utils.letterRatio(Seq(query))}%.2f," +
    f"${Utils.digitRatio(Seq(query))}%.2f," +
    f"${Utils.symbolRatio(Seq(query))}%.2f," +
    f"${headers.size}," +
    f"$getStandardHeaderRatio%.2f," +
    f"$getNonStandardHeaderRatio%.2f," +
    f"${replaceExistingHeaders.map(_.toCsv).mkString(",")}," +
    f"$isPersistentConnection," +
    f"${getHeaderValue("Content-Type")}," +
    f"${body.toCsv}," +
    f"$label"

  /**
    * Indicates if the connection is persistent
    *
    * @return true if the connection is persistent, false otherwise
    */
  def isPersistentConnection: Int = headers.exists(header =>
    header.key.equals("Connection") && header.values.contains("keep-alive")).compareTo(false)

  /**
    * Gets value of a Header
    *
    * @param headerName header's name
    * @return value of a Header, "no_content" if value is empty
    */
  def getHeaderValue(headerName: String): String =
    headers.find(header => header.key.equals(headerName)) match {
      case Some(value) => value.values.head.takeWhile(_ != ',')
      case _ => "no_content"
    }

  /**
    * Gets non standard Header ratio in headers of raw HTTP request
    *
    * @return non standard Header ratio in headers of raw HTTP request
    */
  def getNonStandardHeaderRatio: Double =
    if (headers.isEmpty)
      headers.size
    else
      1 - getStandardHeaderRatio

  /**
    * Gets standard Header ratio in headers of raw HTTP request
    *
    * @return standard Header ratio in headers of raw HTTP request
    */
  def getStandardHeaderRatio: Double =
    if (headers.isEmpty)
      headers.size
    else
      headers.count(element => element.isStandard.equals(1)) / headers.size.toDouble

  /**
    * Represents RawHttpRequest in string format
    *
    * @return RawHttpRequest in string format
    */
  override def toString: String =
    s"$method $url $standard${System.lineSeparator}" +
      s"${headers.mkString(System.lineSeparator)}${System.lineSeparator * 2}" +
      s"${if (body.length > 0) body else ""}${System.lineSeparator}"

  /**
    * Replaces existing Headers in standard Headers list
    *
    * @return standard Headers list with Header of HTTP request
    */
  private def replaceExistingHeaders: Seq[Header] =
    Header.StandardHeaders.map { standard =>
      headers.find(header => header.key.equals(standard.key)) match {
        case Some(value) => value
        case _ => standard
      }
    }

  /**
    * Parses query of a raw HTTP request
    *
    * @param query whole line of query
    * @return list of parameters in query
    */
  private def parseQuery(query: String): Seq[Parameter] = {
    val parameterSeparator = '&'
    if (query.isEmpty)
      Seq[Parameter]()
    else
      query.split(parameterSeparator)
        .map(parseParameter).toMap
        .groupBy(_._1)
        .map(t => new Parameter(t._1, t._2.values.toSeq)).toSeq
  }

  /**
    * Parses parameter of a raw HTTP request
    *
    * @param parameter whole line of an parameter
    * @return parameter's name and header's value
    */
  private def parseParameter(parameter: String): (String, String) = {
    val valueSeparator = "="
    val index = parameter.indexOf(valueSeparator)
    val key = if (index > 0) parameter.substring(0, index) else parameter
    val value = if (index > 0 && parameter.length > index + valueSeparator.length)
      parameter.substring(index + valueSeparator.length)
    else
      ""

    key -> value
  }
}

object RawHttpRequest {

  /**
    * Saves sequence of RawHttpRequests to a CSV file
    *
    * @param path            path of CSV file
    * @param rawHttpRequests sequence of RawHttpRequests
    */
  def saveCsv(path: String, rawHttpRequests: Seq[RawHttpRequest]): Unit =
    Utils.write(path, rawHttpRequests.map(_.toCsv).mkString(System.lineSeparator))

  /**
    * Saves column names to a text file
    *
    * @param path path of text file
    */
  def saveColumnNames(path: String): Unit =
    Utils.write(path, columnNames.mkString(System.lineSeparator))

  /**
    * Gets column names of a RawHttpRequest
    *
    * @return column names of a RawHttpRequest
    */
  def columnNames: Seq[String] = Seq("id", "method") ++
    Path.columnNames ++
    Seq("num_parameters",
      "length_query",
      "printable_characters_ratio_query",
      "non_printable_characters_ratio_query",
      "letter_ratio_query",
      "digit_ratio_query",
      "symbol_ratio_query",
      "num_headers",
      "standard_headers_ratio",
      "non_standard_headers_ratio") ++
    Header.columnNames.flatten ++
    Seq("is_persistent_connection", "content_type") ++
    Body.columnNames ++
    Seq("label")

  /**
    * Displays basic statistics on a list of rawHttpRequests
    *
    * @param rawHttpRequests list of rawHttpRequests
    */
  def basicStatistics(rawHttpRequests: Seq[RawHttpRequest]): Unit = {
    val uniqueSeqMap = Map("path" -> rawHttpRequests.map(_.path.value)
      .distinct.sorted,
      "parameter" -> rawHttpRequests.flatMap(_.parameters.map(_.key))
        .distinct.sorted,
      "header" -> rawHttpRequests.flatMap(_.headers.map(_.key))
        .distinct.sorted,
      "standard" -> rawHttpRequests.map(_.standard)
        .distinct.sorted,
      "MIME type" -> rawHttpRequests.map(_.getHeaderValue("Accept"))
        .distinct.sorted,
      "encoding" -> rawHttpRequests.map(_.getHeaderValue("Accept-Encoding"))
        .distinct.sorted,
      "charset" -> rawHttpRequests.map(_.getHeaderValue("Accept-Charset"))
        .distinct.sorted,
      "language" -> rawHttpRequests.map(_.getHeaderValue("Accept-Language"))
        .distinct.sorted,
      "content type" -> rawHttpRequests.map(_.getHeaderValue("Content-Type"))
        .distinct.sorted)

    println(s"Number of HTTP request :${rawHttpRequests.size}")
    uniqueSeqMap.foreach(t => println(
      s"Number of unique ${t._1} : ${t._2.size}${System.lineSeparator}" +
        s"Sequence of unique ${t._1} : ${t._2.mkString(", ")}"))
  }

  /**
    * Parses raw HTTP requests contain in file
    *
    * @param filename name of file contains raw HTTP requests
    * @param label    type of raw HTTP requests (e.g. normal)
    * @return sequence of raw HTTP requests parsed
    * @throws java.io.FileNotFoundException if an I/O error occurs reading the input stream
    * @throws NoSuchElementException        if HTTP Request is malformed
    */
  def parse(filename: String, label: String): Seq[RawHttpRequest] = {
    val iterator = Source.fromFile(filename).getLines
    val rawHttpRequests = ListBuffer[RawHttpRequest]()

    var nextId: BigInt = 1

    while (iterator.hasNext) {
      val lastRawHttpRequest = parse(iterator, nextId, label)
      rawHttpRequests += lastRawHttpRequest

      nextId = lastRawHttpRequest.id + 1 +
        lastRawHttpRequest.headers.size +
        (if (lastRawHttpRequest.body.value.isEmpty) 2 else 3)
    }

    rawHttpRequests
  }

  /**
    * Parses a raw HTTP request
    *
    * @param iterator   iterator on strings holding raw HTTP request
    * @param lineNumber line number of raw HTTP request
    * @param label      type of raw HTTP request (e.g. normal)
    * @return a raw HTTP request parsed
    * @throws NoSuchElementException if HTTP Request is malformed
    */
  def parse(iterator: Iterator[String], lineNumber: BigInt, label: String): RawHttpRequest = {

    // RFC 2616
    // Request-Line              ; Section 5.1
    val requestLine = iterator.next

    // RFC 2616
    // *(( general-header        ; Section 4.5
    //  | request-header         ; Section 5.3
    //  | entity-header ) CRLF)  ; Section 7.1
    // CRLF
    val requestHeaders = iterator.takeWhile(_.length > 0)
      .map(parseHeader).toMap
      .groupBy(_._1)
      .map(t => Header(t._1, t._2.values.toSeq)).toSeq

    // RFC 2616
    // [ message-body ]          ; Section 4.3
    val messageBody = iterator.takeWhile(_.length > 0).mkString(System.lineSeparator)

    new RawHttpRequest(lineNumber, requestLine, requestHeaders, messageBody, label)
  }

  /**
    * Parses a header of a raw HTTP request
    *
    * @param header whole line of an header
    * @return header's name and header's value
    */
  private def parseHeader(header: String): (String, String) = {
    val valueSeparator = ':'
    val index = header.indexOf(valueSeparator)
    if (index.equals(-1))
      header -> ""
    else
      header.substring(0, index) -> header.substring(index + 2, header.length)
  }

  /**
    * An only value
    * Used to avoid repetition of code
    */
  sealed trait SingleValue {
    require(value != null, "Value must be not null")

    val value: String

    /**
      * Represents SingleValue in CSV format with additional attributes
      *
      * @return SingleValue in CSV format
      */
    def toCsv: String = f"$length," +
      f"$printableCharRatio%.2f," +
      f"$nonPrintableCharRatio%.2f," +
      f"$letterRatio%.2f," +
      f"$digitRatio%.2f," +
      f"$symbolRatio%.2f"

    /**
      * Gets length of value
      *
      * @return length of value
      */
    def length: Int = value.length

    /**
      * Gets printable characters ratio of value
      *
      * @return printable characters ratio of value
      */
    def printableCharRatio: Double = Utils.printableCharRatio(Seq(value))

    /**
      * Gets non printable characters ratio of value
      *
      * @return non printable characters ratio of value
      */
    def nonPrintableCharRatio: Double = Utils.nonPrintableCharRatio(Seq(value))

    /**
      * Gets letters ratio of value
      *
      * @return letters ratio of value
      */
    def letterRatio: Double = Utils.letterRatio(Seq(value))

    /**
      * Gets digits ratio of value
      *
      * @return digits ratio of value
      */
    def digitRatio: Double = Utils.digitRatio(Seq(value))

    /**
      * Gets symbols ratio of value
      *
      * @return symbols ratio of value
      */
    def symbolRatio: Double = Utils.symbolRatio(Seq(value))

    /**
      * Represents SingleValue in string format
      *
      * @return SingleValue in string format
      */
    override def toString: String = s"$value"
  }

  /**
    * A key corresponding to a list of values
    * Used to avoid repetition of code
    */
  sealed trait KeyMultivalued {
    require(key != null && key.nonEmpty, "Key must be not null and non empty")
    require(values != null, "Sequence of values must be not null")

    val key: String
    val values: Seq[String]

    /**
      * Represents KeyMultivalued in CSV format with additional attributes
      *
      * @return KeyMultivalued in CSV format
      */
    def toCsv: String = f"$length," +
      f"$printableCharRatio%.2f," +
      f"$nonPrintableCharRatio%.2f," +
      f"$letterRatio%.2f," +
      f"$digitRatio%.2f," +
      f"$symbolRatio%.2f"

    /**
      * Gets number of values
      *
      * @return number of values
      */
    def length: Int = values.size

    /**
      * Gets total printable characters ratio of values
      *
      * @return total printable characters ratio of values
      */
    def printableCharRatio: Double = Utils.printableCharRatio(values)

    /**
      * Gets total non printable characters ratio of values
      *
      * @return total non printable characters ratio of values
      */
    def nonPrintableCharRatio: Double = Utils.nonPrintableCharRatio(values)

    /**
      * Gets total letters ratio of values
      *
      * @return total letters ratio of values
      */
    def letterRatio: Double = Utils.letterRatio(values)

    /**
      * Gets total digits ratio of values
      *
      * @return total digits ratio of values
      */
    def digitRatio: Double = Utils.digitRatio(values)

    /**
      * Gets total symbols ratio of values
      *
      * @return total symbols ratio of values
      */
    def symbolRatio: Double = Utils.symbolRatio(values)

    /**
      * Represents KeyMultivalued in string format
      *
      * @return KeyMultivalued in string format
      */
    override def toString: String = s"$key -> (${values.mkString(", ")})"
  }

  /**
    * Path of a raw HTTP request
    *
    * @param value value of path of a raw HTTP request
    */
  case class Path(value: String) extends SingleValue {

    /**
      * Represents Path in CSV format with additional attributes
      *
      * @return Path in CSV format
      */
    override def toCsv: String = s"${super.toCsv}," +
      s"$segmentCount," +
      s"$isFile," +
      s"$fileExtension"

    /**
      * Counts number of segments in Path value
      *
      * @return number of segments in Path value
      */
    def segmentCount: Int = value.count(_.equals(Path.Separator))

    /**
      * Indicates if Path's target is a file
      *
      * @return true if Path's target is file, false otherwise
      */
    def isFile: Int = fileExtension.contains(Path.FileExtensionSeparator).compareTo(false)

    /**
      * Gets file extension of Path
      *
      * @return file extension of Path
      */
    def fileExtension: String = {
      val indexSlash = value.lastIndexOf(Path.Separator)
      val indexDot = value.lastIndexOf(Path.FileExtensionSeparator)
      if (indexDot.equals(-1) || indexSlash > indexDot)
        "no_file_extension"
      else
        value.substring(value.lastIndexOf(Path.FileExtensionSeparator))
    }
  }

  /**
    * Parameter of a raw HTTP request
    *
    * @param key    parameter's name of a raw HTTP request
    * @param values parameter's values of a raw HTTP request
    */
  case class Parameter(key: String, values: Seq[String] = Seq[String]()) extends KeyMultivalued

  /**
    * Header of a raw HTTP request
    *
    * @param key    header's name of a raw HTTP request
    * @param values header's values of a raw HTTP request
    */
  case class Header(key: String, values: Seq[String] = Seq[String]()) extends KeyMultivalued {

    /**
      * Represents Header in CSV format with additional attributes
      *
      * @return Header in CSV format
      */
    override def toCsv: String = s"${super.toCsv}," +
      s"$isStandard"

    /**
      * Indicates Header's legitimacy
      *
      * @return true if Header is legitimate, false otherwise
      */
    def isStandard: Int = Header.StandardHeaders.exists(standardHeader =>
      standardHeader.key.equals(key)).compareTo(false)
  }

  /**
    * Body of a raw HTTP request
    *
    * @param value value of message body of a raw HTTP request
    */
  case class Body(value: String) extends SingleValue {

    /**
      * Represents Body in CSV format with additional attributes
      *
      * @return Body in CSV format
      */
    override def toCsv: String = s"${super.toCsv}," +
      s"$lineNumber," +
      s"$wordNumber"

    /**
      * Counts number of lines in Body value
      *
      * @return number of lines in Body value
      */
    def lineNumber: Int = value.split(Body.NewLineRegex).length

    /**
      * Counts number of words in Body value
      *
      * @return number of words in Body value
      */
    def wordNumber: Int = value.split(Body.WordRegex).length
  }

  /**
    * Companion Path of a raw HTTP request
    */
  object Path {

    /**
      * Separator character of a path
      */
    val Separator: Char = '/'

    /**
      * File extension separator character
      */
    val FileExtensionSeparator: Char = '.'

    /**
      * Gets column names of a Path
      *
      * @return column names of a Path
      */
    def columnNames: Seq[String] = Seq("length_path",
      "printable_characters_ratio_path",
      "non_printable_characters_ratio_path",
      "letter_ratio_path",
      "digit_ratio_path",
      "symbol_ratio_path",
      "num_segment",
      "is_file",
      "file_extension")
  }

  /**
    * Companion Parameter of a raw HTTP request
    */
  object Parameter {

    /**
      * Gets column names of a Parameter with parameter's name in suffix
      *
      * @param key parameter's name
      * @return column names of a Parameter
      */
    def columnNames(key: String): Seq[String] = {
      val name = key.replaceAll("\\W", "_").toLowerCase
      Seq(s"length_parameter_$name",
        s"printable_characters_ratio_parameter_$name",
        s"non_printable_characters_ratio_parameter_$name",
        s"letter_ratio_parameter_$name",
        s"digit_ratio_parameter_$name",
        s"symbol_ratio_parameter_$name")
    }
  }

  /**
    * Companion Header of a raw HTTP request
    */
  object Header {

    /**
      * Sequence of standard Headers
      */
    val StandardHeaders: Seq[Header] = Seq("Accept", "Accept-Charset", "Accept-Datetime",
      "Accept-Encoding", "Accept-Language", "Access-Control-Request-Method",
      "Access-Control-Request-Headers", "Authorization", "Cache-Control", "Connection",
      "Content-Length", "Content-MD5", "Content-Type", "Cookie", "Date", "Expect", "From", "Host",
      "If-Match", "If-Modified-Since", "If-None-Match", "If-Range", "If-Unmodified-Since",
      "Max-Forwards", "Origin", "Pragma", "Proxy-Authorization", "Range", "Referer", "TE",
      "User-Agent", "Upgrade", "Via", "Warning"
    ).map(new Header(_))

    /**
      * Gets column names of standard Header list
      *
      * @return column names of all standard Header list
      */
    def columnNames: Seq[Seq[String]] = Header.StandardHeaders.map(header =>
      columnNames(header.key))

    /**
      * Gets column names of a Header with header's name in suffix
      *
      * @param key header's name
      * @return column names of a Header
      */
    def columnNames(key: String): Seq[String] = {
      val name = key.replaceAll("\\W", "_").toLowerCase
      Seq(s"length_header_$name",
        s"printable_characters_ratio_header_$name",
        s"non_printable_characters_ratio_header_$name",
        s"letter_ratio_header_$name",
        s"digit_ratio_header_$name",
        s"symbol_ratio_header_$name",
        s"is_standard_header_$name")
    }
  }

  /**
    * Companion Body of a raw HTTP request
    */
  object Body {

    /**
      * Regex of new line characters
      */
    val NewLineRegex: String = "\r\n|\r|\n"

    /**
      * Regex of word, i.e. alphanumeric characters plus "_"
      */
    val WordRegex: String = "\\w+"

    /**
      * Gets column names of a Body
      *
      * @return column names of a Body
      */
    def columnNames: Seq[String] = Seq("length_body",
      "printable_characters_ratio_body",
      "non_printable_characters_ratio_body",
      "letter_ratio_body",
      "digit_ratio_body",
      "symbol_ratio_body",
      "num_line",
      "num_word")
  }

}
