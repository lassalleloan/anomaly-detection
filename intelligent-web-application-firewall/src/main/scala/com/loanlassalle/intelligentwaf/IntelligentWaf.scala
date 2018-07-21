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
    val normalTraining = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTraining-20.txt",
      "normal")
    val normalTest = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTest-20.txt", "normal")
    val anomalous = RawHttpRequest.parse(s"$resourcesPath/anomalousTrafficTest-20.txt", "anomaly")
    val dataset = normalTraining ++ normalTest ++ anomalous

    println(s"Basic statistics of whole dataset")
    RawHttpRequest.basicStatistics(dataset)
    println

    RawHttpRequest.saveCsv(s"$resourcesPath/train.csv", normalTraining ++ anomalous)
    RawHttpRequest.saveCsv(s"$resourcesPath/validate.csv", normalTest ++ anomalous)
    RawHttpRequest.saveCsv(s"$resourcesPath/test.csv", dataset)

    val columnNames = RawHttpRequest.columnNames
    val training = AnomalyDetector.preProcessing(s"$resourcesPath/train.csv", columnNames: _*)
    val validation = AnomalyDetector.preProcessing(s"$resourcesPath/validate.csv", columnNames: _*)
    val testing = AnomalyDetector.preProcessing(s"$resourcesPath/test.csv", columnNames: _*)

    /**
      * Evaluates KMeans model with all combinations of parameters and determine best model using
      */
    val trainModels = AnomalyDetector.evaluate(training, Array(10), Array(20), Array(1.0E-4))

    println("Evaluation of k-Means models")
    AnomalyDetector.showEvaluationResults(trainModels)
    println

    /**
      * Trains the model
      */
    val bestModel = trainModels.bestModel.asInstanceOf[KMeansModel]
    val distanceToCentroid = AnomalyDetector.train(bestModel, training)

    import AnomalyDetector.SparkSession.implicits._
    val threshold = distanceToCentroid.orderBy($"value".desc).take(23).last

    println(f"Threshold: $threshold%.4f")
    println

    /**
      * Validates the model
      */
    val validationDataFrame = AnomalyDetector.test(bestModel, threshold, validation)

    println("Intelligent WAF on validate.csv")
    println(s"Number of anomalies in file: ${
      validation.filter(row =>
        row.getAs[String]("label")
          .equals("anomaly"))
        .count
    }")
    println(s"Number of anomalies detected: ${validationDataFrame.count}")
    validationDataFrame.show(3)
    println

    println("Confusion Matrix")
    AnomalyDetector.validate(validationDataFrame).foreach(t => println(f"${t._1}: ${t._2}%.2f"))
    println

    /**
      * Tests the model
      */
    val anomalies = AnomalyDetector.test(bestModel, threshold, testing)

    println("Intelligent WAF on test.csv")
    println(s"Number of anomalies in file: ${
      testing.filter(row =>
        row.getAs[String]("label")
          .equals("anomaly"))
        .count
    }")
    println(s"Number of anomalies detected: ${anomalies.count}")
    anomalies.show(3)
  }
}
