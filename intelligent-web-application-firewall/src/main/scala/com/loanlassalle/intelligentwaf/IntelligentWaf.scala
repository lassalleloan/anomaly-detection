package com.loanlassalle.intelligentwaf

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

import scala.io.Source

object IntelligentWaf {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("com").setLevel(Level.ERROR)

    val sparkSession = SparkSession.builder
      .master("local[*]")
      .appName("Spark Intelligent WAF")
      .getOrCreate

    val resourceFolder = getClass.getResource("/csic_2010_http_dataset/").getPath
    val columns = Source.fromFile(resourceFolder + "column_names.txt").getLines().toArray
    val anomalyDetector = new AnomalyDetector(sparkSession)

    val trainingDataFrame = sparkSession.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "normal_traffic_training.csv")
      .toDF(columns: _*)

    //    val normalTestDataFrame = sparkSession.read
    //      .option("inferSchema", value = true)
    //      .option("header", value = false)
    //      .csv(resourceFolder + "normal_traffic_test.csv")
    //      .toDF(columns: _*)

    val anomalousTestDataFrame = sparkSession.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "anomalous_traffic_test.csv")
      .toDF(columns: _*)

    //    anomalyDetector.clusteringTake(trainingDataFrame)

    val (kMeansModel, threshold) = anomalyDetector.train(trainingDataFrame)
    val anomalies = anomalyDetector.test(kMeansModel, anomalousTestDataFrame, threshold)

    println("Intelligent WAF")
    println(s"Number of anomalies in file: ${anomalousTestDataFrame.count()}")
    println(s"Number of anomalies detected: ${anomalies.count()}")
    println(anomalies.first)

    sparkSession.stop
  }
}
