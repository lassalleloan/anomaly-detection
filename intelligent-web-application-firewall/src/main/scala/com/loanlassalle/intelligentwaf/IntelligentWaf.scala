package com.loanlassalle.intelligentwaf

import org.apache.log4j.{Level, Logger}

object IntelligentWaf {
  def main(args: Array[String]): Unit = {

    /**
      * Disables some types of logger message
      */
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("com").setLevel(Level.ERROR)

    val resourcesPath = getClass.getResource("/csic_2010_http_dataset").getPath

    /**
      * Raw-processes of raw data for anomaly detection
      */
//    val dataset = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTraining.txt", "normal")++
//      RawHttpRequest.parse(s"$resourcesPath/anomalousTrafficTest.txt", "anomaly") ++
//      RawHttpRequest.parse(s"$resourcesPath/normalTrafficTest.txt", "normal")
//
//    println(s"Basic statistics of dataset.csv")
//    RawHttpRequest.basicStatistics(dataset)
//    RawHttpRequest.saveCsv(s"$resourcesPath/dataset.csv", dataset)
//    println

    val columnNames = RawHttpRequest.columnNames
    val Array(training, validation, testing) = AnomalyDetector
      .preProcessing(s"$resourcesPath/dataset.csv", columnNames: _*)
      .randomSplit(Array(0.7, 0.2, 0.1))

    /**
      * Evaluates KMeans model with all combinations of parameters and determine best model using
      */
//    AnomalyDetector.showEvaluationResults(AnomalyDetector.evaluate(training))
//    println

    /**
      * Trains the model
      */
    val (model, threshold) = AnomalyDetector.train(training)

    /**
      * Validates the model
      */
    val validationDataFrame = AnomalyDetector.test(model, threshold, validation)
    println(s"Number of rows in file: ${validationDataFrame.count}")
    println(s"Number of anomalies detected: ${validationDataFrame.count}")
    AnomalyDetector.validate(validationDataFrame).foreach(t => println(f"${t._1}: ${t._2}%.2f"))

    /**
      * Tests the model
      */
//    val anomalies = AnomalyDetector.test(model, threshold, testing)
//
//    println("Intelligent WAF on normal_traffic_test.csv")
//    println(s"Number of rows in file: ${test.count}")
//    println(s"Number of anomalies detected: ${anomalies.count}")
//    anomalies.show(3)
  }
}
