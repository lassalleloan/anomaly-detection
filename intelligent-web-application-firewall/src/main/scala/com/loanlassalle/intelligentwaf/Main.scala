package com.loanlassalle.intelligentwaf

import org.apache.log4j._
import org.apache.spark.sql.SparkSession

object Main {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Spark Intelligent WAF")
      .getOrCreate()
    val sc = spark.sparkContext
    val filename = "/csic_2010_http_dataset/normalTrafficTraining-10-requests.txt"
    val rawHttpRequests = RawHttpRequest.parseRawRequestFile(getClass.getResource(filename).getPath)

    println("num lines: " + sc.textFile(getClass.getResource(filename).getPath).count())
    rawHttpRequests.foreach(println)

    sc.stop()
    spark.stop()
  }

}
