package com.loanlassalle.intelligentwaf

import java.awt.Desktop
import java.io.File

object DataPreprocessing {
  def main(args: Array[String]): Unit = {
    val resourceFolder = getClass.getResource("/csic_2010_http_dataset/").getPath

    /**
      * Transforms raw HTTP requests to RawHttpRequest
      */
    val rawHttpRequestsTraining = RawHttpRequest.parse(resourceFolder + "/normalTrafficTraining.txt")
    val rawHttpRequestsTest = RawHttpRequest.parse(resourceFolder + "/normalTrafficTest.txt")
    val rawHttpRequestsAnomalous = RawHttpRequest.parse(resourceFolder + "/anomalousTrafficTest.txt")

    /**
      * Displays some basic statistics
      */
    println("Basic statistics of normalTrafficTraining.txt")
    RawHttpRequest.basicStatistics(rawHttpRequestsTraining)
    println
    println("Basic statistics of normalTrafficTest.txt")
    RawHttpRequest.basicStatistics(rawHttpRequestsTest)
    println
    println("Basic statistics of anomalousTrafficTest.txt")
    RawHttpRequest.basicStatistics(rawHttpRequestsAnomalous)

    /**
      * Saves information in CSV format
      */
    RawHttpRequest.saveColumnNames(resourceFolder + "/column_names.txt")
    RawHttpRequest.saveCsv(resourceFolder + "/normal_traffic_training.csv", rawHttpRequestsTraining)
    RawHttpRequest.saveCsv(resourceFolder + "/normal_traffic_test.csv", rawHttpRequestsTest)
    RawHttpRequest.saveCsv(resourceFolder + "/anomalous_traffic_test.csv", rawHttpRequestsAnomalous)

    /**
      * Open targeted directory
      */
    Desktop.getDesktop.open(new File(resourceFolder))
  }
}
