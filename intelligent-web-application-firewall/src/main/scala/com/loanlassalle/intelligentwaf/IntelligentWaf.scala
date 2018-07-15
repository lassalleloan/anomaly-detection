package com.loanlassalle.intelligentwaf

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.feature.{OneHotEncoder, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.io.Source
import scala.util.Random

object IntelligentWaf {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("com").setLevel(Level.ERROR)

    val resourceFolder = getClass.getResource("/csic_2010_http_dataset/").getPath

    val spark = SparkSession.builder
      .master("local[*]")
      .appName("Spark Intelligent Web Application Firewall")
      .getOrCreate

    val columns = Source.fromFile(resourceFolder + "columns_name.txt").getLines().toArray

    val trainingDataFrame = spark.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "normal_traffic_training.csv")
      .toDF(columns: _*)

    val normalTestDataFrame = spark.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "normal_traffic_test.csv")
      .toDF(columns: _*)

    val anomalousTestDataFrame = spark.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "anomalous_traffic_test.csv")
      .toDF(columns: _*)
    
    //    clusteringTake(trainingDataFrame)

    val anomaliesTest = intrusionDetector(spark, trainingDataFrame, normalTestDataFrame)
    println("Intrusion detection with normal data")
    anomaliesTest.take(3).foreach(println)
    println

    val anomalies = intrusionDetector(spark, trainingDataFrame, anomalousTestDataFrame)
    println("Intrusion detection with anomaly data")
    anomalies.take(3).foreach(println)
    println(s"Number of anomalies in file: ${anomalousTestDataFrame.count()}")
    println(s"Number of anomalies detected: ${anomalies.count()}")

    spark.stop
  }

  def intrusionDetector(spark: SparkSession,
                        trainingDataFrame: DataFrame,
                        testDataFrame: DataFrame): DataFrame = {
    import spark.implicits._
    val model = fitPipeline(trainingDataFrame, 210)

    val kMeansModel = model.stages.last.asInstanceOf[KMeansModel]
    val centroids = kMeansModel.clusterCenters

    val clustered = model.transform(testDataFrame)
    val threshold = clustered.select("cluster", "scaledFeatureVector").as[(Int, Vector)]
      .map { case (cluster, vec) => Vectors.sqdist(centroids(cluster), vec) }
      .orderBy($"value".desc)
      .take(100)
      .last

    val originalCols = trainingDataFrame.columns

    clustered.filter { row =>
      val cluster = row.getAs[Int]("cluster")
      val vec = row.getAs[Vector]("scaledFeatureVector")
      Vectors.sqdist(centroids(cluster), vec) >= threshold
    }.select(originalCols.head, originalCols.tail: _*)
  }

  def clusteringTake(data: DataFrame): Unit = {
    (60 to 270 by 30).map(k => (k, clusteringScore(data, k)))
      .foreach(println)
  }

  def clusteringScore(data: DataFrame, k: Int): Double = {
    val model = fitPipeline(data, k)
    model.stages.last.asInstanceOf[KMeansModel].computeCost(model.transform(data)) / data.count()
  }

  def fitPipeline(data: DataFrame, k: Int): PipelineModel = {
    val (methodEncoder, methodVecCol) = oneHotPipeline("method")
    val (fileExtensionEncoder, fileExtensionVecCol) = oneHotPipeline("file_extension")
    val (contentTypeEncoder, contentTypeVecCol) = oneHotPipeline("content_type")

    // Original columns, without label / string columns, but with new vector encoded cols
    val assembleCols = Set(data.columns: _*) --
      Seq("method", "file_extension", "content_type") ++
      Seq(methodVecCol, fileExtensionVecCol, contentTypeVecCol)

    val assembler = new VectorAssembler()
      .setInputCols(assembleCols.toArray)
      .setOutputCol("featureVector")

    val scaler = new StandardScaler()
      .setWithStd(true)
      .setWithMean(false)
      .setInputCol("featureVector")
      .setOutputCol("scaledFeatureVector")

    val kMeans = new KMeans()
      .setSeed(Random.nextLong())
      .setK(k)
      .setMaxIter(40)
      .setTol(1.0e-5)
      .setPredictionCol("cluster")
      .setFeaturesCol("scaledFeatureVector")

    new Pipeline().setStages(Array(methodEncoder, fileExtensionEncoder, contentTypeEncoder,
      assembler, scaler, kMeans)).fit(data)
  }

  def oneHotPipeline(column: String): (Pipeline, String) = {
    val indexer = new StringIndexer()
      .setInputCol(column)
      .setOutputCol(column + "_index")
      .setHandleInvalid("skip")

    val encoder = new OneHotEncoder()
      .setInputCol(column + "_index")
      .setOutputCol(column + "_vector")

    val pipeline = new Pipeline().setStages(Array(indexer, encoder))
    pipeline -> (column + "_vector")
  }
}
