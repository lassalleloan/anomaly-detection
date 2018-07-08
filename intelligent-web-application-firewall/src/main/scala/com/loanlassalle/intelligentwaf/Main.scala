package com.loanlassalle.intelligentwaf

import org.apache.log4j._
import org.apache.spark.sql.SparkSession

object Main {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)

    val filename = "/csic_2010_http_dataset/normalTrafficTraining-10-requests.txt"

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Spark Intelligent WAF")
      .getOrCreate()
    val sc = spark.sparkContext

    val rawHttpRequests = RawHttpRequest.parseRawRequestFile(getClass.getResource(filename).getPath)

    println("Basic statistics")
    println("Number of HTTP request: " + rawHttpRequests.size)
    println("Number of unique path : " + rawHttpRequests.map(_.path).distinct.size)
    println("List of unique path : " + rawHttpRequests.map(_.path).distinct)
    println("Number of unique parameter : " + rawHttpRequests.flatMap(_.parameters.keySet).distinct.size)
    println("List of unique parameter : " + rawHttpRequests.flatMap(_.parameters.keySet).distinct)
    println("Number of unique header : " + rawHttpRequests.flatMap(_.requestHeaders.keySet).distinct.size)
    println("List of unique header : " + rawHttpRequests.flatMap(_.requestHeaders.keySet).distinct)
    println()
    println("Dataset")

    println(rawHttpRequests.head.fileExtension)

    sc.stop()
    spark.stop()
  }

}
