package com.loanlassalle.intelligentwaf

import java.awt.Desktop
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

object DataPreprocessing {
  def main(args: Array[String]): Unit = {
    val resourceFolder = getClass.getResource("/csic_2010_http_dataset/").getPath

    Files.write(Paths.get(resourceFolder + "/column_names.txt"),
      RawHttpRequest.columnNames.split(',').mkString("\n").getBytes(StandardCharsets.UTF_8))

    val rawHttpRequestsTraining = RawHttpRequest.parseFile(resourceFolder + "/normalTrafficTraining.txt")
    val rawHttpRequestsTest = RawHttpRequest.parseFile(resourceFolder + "/normalTrafficTest.txt")
    val rawHttpRequestsAnomalous = RawHttpRequest.parseFile(resourceFolder + "/anomalousTrafficTest.txt")
    val basicsStatisticTraining = RawHttpRequest.basicStatistics(rawHttpRequestsTraining)
    println(basicsStatisticTraining)

    Files.write(Paths.get(resourceFolder + "/basics_statistic.txt"),
      basicsStatisticTraining.getBytes(StandardCharsets.UTF_8))
    Files.write(Paths.get(resourceFolder + "/normal_traffic_training.csv"),
      rawHttpRequestsTraining.map(_.toCsv).mkString(System.lineSeparator()).getBytes
      (StandardCharsets.UTF_8))
    Files.write(Paths.get(resourceFolder + "/normal_traffic_test.csv"),
      rawHttpRequestsTest.map(_.toCsv).mkString(System.lineSeparator()).getBytes(StandardCharsets.UTF_8))
    Files.write(Paths.get(resourceFolder + "/anomalous_traffic_test.csv"),
      rawHttpRequestsAnomalous.map(_.toCsv).mkString(System.lineSeparator()).getBytes(StandardCharsets.UTF_8))

    Desktop.getDesktop.open(new File(resourceFolder))
  }
}
