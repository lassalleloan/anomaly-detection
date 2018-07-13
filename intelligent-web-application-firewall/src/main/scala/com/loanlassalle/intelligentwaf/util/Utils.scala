package com.loanlassalle.intelligentwaf.util

object Utils {

  def nonPrintableCharRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + nonPrintableCharCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  def nonPrintableCharCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      str.length - printableCharCount(str)

  def printableCharCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      "[ -~]".r.findAllIn(str).length

  def printableCharRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + printableCharCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  def symbolRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + symbolCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  def symbolCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      printableCharCount(str) - letterCount(str) - digitCount(str)

  def letterRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + letterCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  def letterCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      str.count(_.isLetter)

  def digitRatio(seq: Seq[String]): Double =
    if (seq.isEmpty || seq.forall(_.isEmpty))
      0
    else
      seq.foldLeft(0)((sum, value) => sum + digitCount(value)) /
        seq.foldLeft(0.0)((sum, value) => sum + value.length)

  def digitCount(str: String): Int =
    if (str.isEmpty)
      str.length
    else
      str.count(_.isDigit)

}
