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
        AnomalyDetector.rawPreProcessing(resourcesPath,
          "normalTrafficTraining.txt",
          "normal_traffic_training.csv")
        println
        AnomalyDetector.rawPreProcessing(resourcesPath,
          "anomalousTrafficTest.txt",
          "anomalous_traffic_test.csv")
        println

    val columnNames = RawHttpRequest.columnNames
    val training = AnomalyDetector
      .preProcessing(s"$resourcesPath/normal_traffic_training.csv", columnNames: _*)

    /**
      * Evaluates KMeans model with all combinations of parameters and determine best model using
      */
        AnomalyDetector.showEvaluationResults(AnomalyDetector.evaluate(training))

    /**
      * Trains and validates the model
      */
    val (model, threshold) = AnomalyDetector.train(training)

    /**
      * Tests the model
      */
    val test = AnomalyDetector
      .preProcessing(s"$resourcesPath/normal_traffic_test.csv", columnNames: _*)
    val anomalies = AnomalyDetector.test(model, threshold, test)

    println("Intelligent WAF on normal_traffic_test.csv")
    println(s"Number of rows in file: ${test.count}")
    println(s"Number of anomalies detected: ${anomalies.count}")
    anomalies.show(3)
  }
}
