package com.loanlassalle.intelligentwaf

import java.net.URL

import com.loanlassalle.intelligentwaf.RawHttpRequest._
import com.loanlassalle.intelligentwaf.util.Utils

import scala.collection.mutable.ListBuffer
import scala.io.Source

case class RawHttpRequest(requestLine: String,
                          requestHeaders: List[Header],
                          messageBody: String) {
  require(requestLine != null && requestLine.count(_.equals(' ')).equals(2))
  require(requestHeaders != null)
  require(messageBody != null)

  private val requestLineSplit: Array[String] = requestLine.split(" ")
  private val url: URL = try {new URL(requestLineSplit.tail.head)}catch {case _: Throwable => null}
  private val query: String = if (url == null || url.getQuery == null) "" else url.getQuery
  private val pathValue: String = if (url == null || url.getPath == null) "" else url.getPath

  val method: String = requestLineSplit.head
  val path: Path = Path(pathValue)
  val parameters: List[Parameter] = parseQuery(query)
  val standard: String = requestLineSplit.last
  val headers: List[Header] = requestHeaders
  val body: Body = new Body(messageBody)

  def csv: String = s"$method," +
    s"${path.csv}," +
    s"${parameters.size}," +
    s"${query.length}," +
    f"${Utils.printableCharRatio(Seq(query))}%.2f,${Utils.nonPrintableCharRatio(Seq(query))}%.2f," +
    f"${Utils.letterRatio(Seq(query))}%.2f,${Utils.digitRatio(Seq(query))}%.2f," +
    f"${Utils.symbolRatio(Seq(query))}%.2f," +
    f"${headers.size},$getStandardHeaderRatio%.2f,$getNonStandardHeaderRatio%.2f," +
    s"${replaceExistingHeaders.map(_.csv).mkString(",")}," +
    s"$isPersistentConnection,${getHeaderValue("Content-Type")}," +
    s"${body.csv}"

  def isPersistentConnection: Int = headers.exists(header =>
    header.key.equals("Connection") && header.values.contains("keep-alive")).compareTo(false)

  def getHeaderValue(headerName: String): String =
    headers.find(header => header.key.equals(headerName)) match {
      case Some(value) => value.values.head.takeWhile(_ != ',')
      case _ => "no_content"
    }

  def getStandardHeaderRatio: Double =
    if (headers.isEmpty)
      headers.size
    else
      headers.count(element => element.isStandard.equals(1)) / headers.size.toDouble

  def getNonStandardHeaderRatio: Double =
    if (headers.isEmpty)
      headers.size
    else
      1 - getStandardHeaderRatio

  override def toString: String =
    s"$method $url $standard" + System.lineSeparator() +
      s"${headers.mkString(System.lineSeparator())}" + System.lineSeparator() + System.lineSeparator() +
      (if (body.length > 0) body else "") + System.lineSeparator()

  private def replaceExistingHeaders: List[Header] =
    Header.StandardHeaders.map { standard =>
      headers.find(header => header.key.equals(standard.key)) match {
        case Some(value) => value
        case _ => standard
      }
    }

  private def parseQuery(query: String): List[Parameter] = {
    val parameterSeparator = '&'

    if (query.isEmpty)
      List[Parameter]()
    else
      query.split(parameterSeparator)
        .map(parseParameter).toMap
        .groupBy(_._1)
        .map(t => new Parameter(t._1, t._2.values.toList)).toList
  }

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
  def basicStatistics(rawHttpRequests: Seq[RawHttpRequest]): String = {
    val uniquePaths = rawHttpRequests.map(_.path.value)
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueParameters = rawHttpRequests.flatMap(_.parameters.map(_.key))
      .distinct.sortWith((a, b) => a < b)
    val uniqueHeaders = rawHttpRequests.flatMap(_.headers.map(_.key))
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueStandard = rawHttpRequests.map(_.standard)
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueMimeType = rawHttpRequests.map(_.getHeaderValue("Accept"))
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueEncoding = rawHttpRequests.map(_.getHeaderValue("Accept-Encoding"))
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueCharset = rawHttpRequests.map(_.getHeaderValue("Accept-Charset"))
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueLanguage = rawHttpRequests.map(_.getHeaderValue("Accept-Language"))
      .distinct
      .sortWith((a, b) => a < b)
    val uniqueContentType = rawHttpRequests.map(_.getHeaderValue("Content-Type"))
      .distinct
      .sortWith((a, b) => a < b)

    "Basic statistics\n" +
      s"Number of HTTP request         : ${rawHttpRequests.size}\n" +
      s"Number of unique path          : ${uniquePaths.size}\n" +
      s"List of unique path            : ${uniquePaths.mkString(", ")}\n" +
      s"Number of unique parameter     : ${uniqueParameters.size}\n" +
      s"List of unique parameter       : ${uniqueParameters.mkString(", ")}\n" +
      s"Number of unique header        : ${uniqueHeaders.size}\n" +
      s"List of unique header          : ${uniqueHeaders.mkString(", ")}\n" +
      s"Number of unique standard      : ${uniqueStandard.size}\n" +
      s"List of unique standard        : ${uniqueStandard.mkString(", ")}\n" +
      s"Number of unique MIME type     : ${uniqueMimeType.size}\n" +
      s"List of unique MIME type       : ${uniqueMimeType.mkString(", ")}\n" +
      s"Number of unique encoding      : ${uniqueEncoding.size}\n" +
      s"List of unique encoding        : ${uniqueEncoding.mkString(", ")}\n" +
      s"Number of unique charset       : ${uniqueCharset.size}\n" +
      s"List of unique charset         : ${uniqueCharset.mkString(", ")}\n" +
      s"Number of unique language      : ${uniqueLanguage.size}\n" +
      s"List of unique language        : ${uniqueLanguage.mkString(", ")}\n" +
      s"Number of unique content type  : ${uniqueContentType.size}\n" +
      s"List of unique content type    : ${uniqueContentType.mkString(", ")}"
  }

  def columnNames: String = s"method," +
    s"${Path.columnNames}," +
    s"num_parameters," +
    s"length_query," +
    s"printable_characters_ratio_query,non_printable_characters_ratio_query," +
    s"letter_ratio_query,digit_ratio_query," +
    s"symbol_ratio_query," +
    s"num_headers,standard_headers_ratio,non_standard_headers_ratio," +
    (for {index <- Header.StandardHeaders.indices} yield Header.columnNames(index) + ",").mkString +
    s"is_persistent_connection,content_type," +
    s"${Body.columnNames}"

  /**
    * Parse raw HTTP requests contain in file
    *
    * @param filename name of file contains raw HTTP requests
    * @throws java.io.FileNotFoundException if an I/O error occurs reading the input stream
    * @throws NoSuchElementException        if HTTP Request is malformed
    */
  def parseFile(filename: String): ListBuffer[RawHttpRequest] = {
    val iterator = Source.fromFile(filename).getLines
    val httpRequests = ListBuffer[RawHttpRequest]()

    while (iterator.hasNext) {
      httpRequests += parse(iterator)
    }

    httpRequests
  }

  /**
    * Parse a raw HTTP request
    *
    * @param iterator iterator on strings holding raw HTTP request
    * @throws NoSuchElementException if HTTP Request is malformed
    */
  def parse(iterator: Iterator[String]): RawHttpRequest = {

    // Request-Line              ; Section 5.1
    val requestLine = iterator.next()

    // *(( general-header        ; Section 4.5
    //  | request-header         ; Section 5.3
    //  | entity-header ) CRLF)  ; Section 7.1
    // CRLF
    val requestHeaders = iterator.takeWhile(_.length > 0)
      .map(parseHeader).toMap
      .groupBy(_._1)
      .map(t => Header(t._1, t._2.values.toList)).toList

    // [ message-body ]          ; Section 4.3
    val messageBody = iterator.takeWhile(_.length > 0).mkString(System.lineSeparator())

    RawHttpRequest(requestLine, requestHeaders, messageBody)
  }

  private def parseHeader(header: String): (String, String) = {
    val valueSeparator = ':'
    val index = header.indexOf(valueSeparator)
    if (index == -1)
      header -> ""
    else
      header.substring(0, index) -> header.substring(index + 2, header.length())
  }

  sealed trait SingleValue {
    require(value != null)

    val value: String

    def csv: String = f"$length,$printableCharRatio%.2f," +
      f"$nonPrintableCharRatio%.2f,$letterRatio%.2f," +
      f"$digitRatio%.2f,$symbolRatio%.2f"

    def length: Int = value.length

    def printableCharRatio: Double = Utils.printableCharRatio(Seq(value))

    def nonPrintableCharRatio: Double = Utils.nonPrintableCharRatio(Seq(value))

    def letterRatio: Double = Utils.letterRatio(Seq(value))

    def digitRatio: Double = Utils.digitRatio(Seq(value))

    def symbolRatio: Double = Utils.symbolRatio(Seq(value))

    override def toString: String = s"$value"
  }

  sealed trait KeyMultivalued {
    require(key != null && key.nonEmpty)
    require(values != null)

    val key: String
    val values: List[String]

    def csv: String = f"$length,$printableCharRatio%.2f," +
      f"$nonPrintableCharRatio%.2f,$letterRatio%.2f," +
      f"$digitRatio%.2f,$symbolRatio%.2f"

    def length: Int = values.size

    def printableCharRatio: Double = Utils.printableCharRatio(values)

    def nonPrintableCharRatio: Double = Utils.nonPrintableCharRatio(values)

    def letterRatio: Double = Utils.letterRatio(values)

    def digitRatio: Double = Utils.digitRatio(values)

    def symbolRatio: Double = Utils.symbolRatio(values)

    override def toString: String = s"$key -> (${values.mkString(", ")})"
  }

  case class Path(value: String) extends SingleValue {
    override def csv: String = super.csv + s",$segmentCount,$isFile,$fileExtension"

    def segmentCount: Int = value.count(_.equals(Path.Separator))

    def isFile: Int = value.substring(value.lastIndexOf(Path.Separator) + 1)
      .contains(Path.ExtensionSeparator)
      .compareTo(false)

    def fileExtension: String = value.substring(value.lastIndexOf(Path.ExtensionSeparator) + 1)
  }

  case class Parameter(key: String, values: List[String] = List[String]()) extends KeyMultivalued

  case class Header(key: String, values: List[String] = List[String]()) extends KeyMultivalued {
    override def csv: String = super.csv + s",$isStandard"

    def isStandard: Int = Header.StandardHeaders.exists(standardHeader =>
      standardHeader.key.equals(key)).compareTo(false)
  }

  case class Body(value: String) extends SingleValue {
    override def csv: String = super.csv + s",$lineNumber,$wordNumber"

    def lineNumber: Int = value.split(Body.newLineRegex).length

    def wordNumber: Int = value.split(Body.wordRegex).length
  }

  object Path {
    val Separator: Char = '/'
    val ExtensionSeparator: Char = '.'

    def columnNames: String = "length_path,printable_characters_ratio_path," +
      "non_printable_characters_ratio_path,letter_ratio_path," +
      "digit_ratio_path,symbol_ratio_path," +
      "num_segment,is_file," +
      "file_extension"
  }

  object Parameter {
    def columnNames(index: Int): String = s"length_parameter_$index,printable_characters_ratio_parameter_$index," +
      s"non_printable_characters_ratio_parameter_$index,letter_ratio_parameter_$index," +
      s"digit_ratio_parameter_$index,symbol_ratio_parameter"
  }

  object Header {
    val StandardHeaders: List[Header] = List("Accept", "Accept-Charset", "Accept-Datetime", "Accept-Encoding",
      "Accept-Language", "Access-Control-Request-Method", "Access-Control-Request-Headers", "Authorization",
      "Cache-Control", "Connection", "Content-Length", "Content-MD5",
      "Content-Type", "Cookie", "Date", "Expect",
      "From", "Host", "If-Match", "If-Modified-Since",
      "If-None-Match", "If-Range", "If-Unmodified-Since", "Max-Forwards",
      "Origin", "Pragma", "Proxy-Authorization", "Range",
      "Referer", "TE", "User-Agent", "Upgrade",
      "Via", "Warning").map(new Header(_))

    def columnNames(index: Int): String = s"length_header_$index,printable_characters_ratio_header_$index," +
      s"non_printable_characters_ratio_header_$index,letter_ratio_header_$index," +
      s"digit_ratio_header_$index,symbol_ratio_header_$index," +
      s"is_standard_header_$index"
  }

  object Body {
    val newLineRegex: String = "\r\n|\r|\n"
    val wordRegex: String = "\\w+"

    def columnNames: String = "length_body,printable_characters_ratio_body," +
      "non_printable_characters_ratio_body,letter_ratio_body,digit_ratio_body," +
      "symbol_ratio_body,num_line,num_word"
  }

}
