package com.loanlassalle.intelligentwaf.util

/**
  * A toolbox for basics statistic
  */
object Utils {

  /**
    * Gets total non printable characters ratio of sequence of string
    *
    * @param seq sequence of string
    * @return total non printable characters ratio of sequence of string
    */
  def nonPrintableCharRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + nonPrintableCharCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  /**
    * Counts number of non printable characters in a string
    * @param str string
    * @return number of non printable characters in a string
    */
  def nonPrintableCharCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      str.length - printableCharCount(str)

  /**
    * Counts number of printable characters in a string
    * @param str string
    * @return number of printable characters in a string
    */
  def printableCharCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      "[ -~]".r.findAllIn(str).length

  /**
    * Gets total printable characters ratio of sequence of string
    *
    * @param seq sequence of string
    * @return total printable characters ratio of sequence of string
    */
  def printableCharRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + printableCharCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  /**
    * Gets total symbols ratio of sequence of string
    *
    * @param seq sequence of string
    * @return total symbols ratio of sequence of string
    */
  def symbolRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + symbolCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  /**
    * Counts number of symbols in a string
    * @param str string
    * @return number of symbols in a string
    */
  def symbolCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      printableCharCount(str) - letterCount(str) - digitCount(str)

  /**
    * Gets total letters ratio of sequence of string
    *
    * @param seq sequence of string
    * @return total letters ratio of sequence of string
    */
  def letterRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + letterCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  /**
    * Counts number of letters in a string
    * @param str string
    * @return number of letters in a string
    */
  def letterCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      str.count(_.isLetter)

  /**
    * Gets total digits ratio of sequence of string
    *
    * @param seq sequence of string
    * @return total digits ratio of sequence of string
    */
  def digitRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + digitCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  /**
    * Counts number of digits in a string
    * @param str string
    * @return number of digits in a string
    */
  def digitCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      str.count(_.isDigit)
}
