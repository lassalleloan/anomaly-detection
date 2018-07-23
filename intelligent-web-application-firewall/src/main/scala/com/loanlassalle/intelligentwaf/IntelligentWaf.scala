package com.loanlassalle.intelligentwaf

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.clustering.KMeansModel

object IntelligentWaf {
  def main(args: Array[String]): Unit = {

    /**
      * Disables some types of logger message
      */
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("com").setLevel(Level.ERROR)

    val resourcesPath = getClass.getResource("/csic_2010_http_dataset").getPath

    /**
      * Pre-processes of raw data for anomaly detection
      */
    val normalTraining = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTraining.txt",
      "normal")
    val normalTest = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTest.txt",
      "normal")
    val anomalous = RawHttpRequest.parse(s"$resourcesPath/anomalousTrafficTest.txt",
      "anomaly")

    println(s"Basic statistics of all dataset")
    RawHttpRequest.basicStatistics(normalTraining ++ normalTest ++ anomalous)
    println

    RawHttpRequest.saveCsv(s"$resourcesPath/train.csv", normalTraining ++ anomalous)
    RawHttpRequest.saveCsv(s"$resourcesPath/test.csv", normalTest ++ anomalous)

    val columnNames = RawHttpRequest.columnNames
    val training = AnomalyDetector.preProcessing(s"$resourcesPath/train.csv", columnNames: _*)
    val testing = AnomalyDetector.preProcessing(s"$resourcesPath/test.csv", columnNames: _*)

    /**
      * Tunes KMeans model with all combinations of parameters and determine the best
      * model
      * using
      */
    val trainModels = AnomalyDetector.tune(training,
      30 to 270 by 30,
      20 to 60 by 10,
      Array(1.0E-4, 1.0E-5, 1.0E-6))

    println("Tuning of k-Means model")
    AnomalyDetector.showTuningResults(trainModels)
    println

    /**
      * Evaluates the model
      */
    println("Evaluation of k-Means model")
    val bestModel = trainModels.bestModel.asInstanceOf[KMeansModel]
    val metrics = AnomalyDetector.evaluate(bestModel, testing)
    AnomalyDetector.showEvaluationResults(metrics)
    println

    /**
      * Tests the model
      */
    val threshold = 4.0
    val anomalies = AnomalyDetector.test(bestModel, threshold, testing)

    println("Intelligent WAF on test.csv")
    println(s"Number of anomalies in file: ${
      testing.filter(row =>
        row.getAs[String]("label")
          .equals("anomaly"))
        .count
    }")
    println(s"Number of anomalies detected: ${anomalies.count}")
    println(s"Number of actual anomalies detected: ${
      anomalies.filter(row =>
        row.getAs[String]("label")
          .equals("anomaly"))
        .count
    }")
    anomalies.show(3)
  }
}
