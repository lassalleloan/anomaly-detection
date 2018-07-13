package com.loanlassalle.intelligentwaf

import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.feature.{OneHotEncoder, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

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

    val trainingDataFrame = spark.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "normal_traffic_training.csv")
      .toDF("method", "length_path", "printable_characters_ratio_path",
        "non_printable_characters_ratio_path", "letter_ratio_path", "digit_ratio_path",
        "symbol_ratio_path", "num_segment", "is_file", "file_extension", "num_parameters", "length_query",
        "printable_characters_ratio_query", "non_printable_characters_ratio_query", "letter_ratio_query",
        "digit_ratio_query", "symbol_ratio_query", "num_headers", "standard_headers_ratio",
        "non_standard_headers_ratio", "length_header_0", "printable_characters_ratio_header_0",
        "non_printable_characters_ratio_header_0", "letter_ratio_header_0", "digit_ratio_header_0",
        "symbol_ratio_header_0", "is_standard_header_0", "length_header_1",
        "printable_characters_ratio_header_1", "non_printable_characters_ratio_header_1",
        "letter_ratio_header_1", "digit_ratio_header_1", "symbol_ratio_header_1", "is_standard_header_1",
        "length_header_2", "printable_characters_ratio_header_2",
        "non_printable_characters_ratio_header_2", "letter_ratio_header_2", "digit_ratio_header_2",
        "symbol_ratio_header_2", "is_standard_header_2", "length_header_3",
        "printable_characters_ratio_header_3", "non_printable_characters_ratio_header_3",
        "letter_ratio_header_3", "digit_ratio_header_3", "symbol_ratio_header_3", "is_standard_header_3",
        "length_header_4", "printable_characters_ratio_header_4",
        "non_printable_characters_ratio_header_4", "letter_ratio_header_4", "digit_ratio_header_4",
        "symbol_ratio_header_4", "is_standard_header_4", "length_header_5",
        "printable_characters_ratio_header_5", "non_printable_characters_ratio_header_5",
        "letter_ratio_header_5", "digit_ratio_header_5", "symbol_ratio_header_5", "is_standard_header_5",
        "length_header_6", "printable_characters_ratio_header_6",
        "non_printable_characters_ratio_header_6", "letter_ratio_header_6", "digit_ratio_header_6",
        "symbol_ratio_header_6", "is_standard_header_6", "length_header_7",
        "printable_characters_ratio_header_7", "non_printable_characters_ratio_header_7",
        "letter_ratio_header_7", "digit_ratio_header_7", "symbol_ratio_header_7", "is_standard_header_7",
        "length_header_8", "printable_characters_ratio_header_8",
        "non_printable_characters_ratio_header_8", "letter_ratio_header_8", "digit_ratio_header_8",
        "symbol_ratio_header_8", "is_standard_header_8", "length_header_9",
        "printable_characters_ratio_header_9", "non_printable_characters_ratio_header_9",
        "letter_ratio_header_9", "digit_ratio_header_9", "symbol_ratio_header_9", "is_standard_header_9",
        "length_header_10", "printable_characters_ratio_header_10",
        "non_printable_characters_ratio_header_10", "letter_ratio_header_10", "digit_ratio_header_10",
        "symbol_ratio_header_10", "is_standard_header_10", "length_header_11",
        "printable_characters_ratio_header_11", "non_printable_characters_ratio_header_11",
        "letter_ratio_header_11", "digit_ratio_header_11", "symbol_ratio_header_11",
        "is_standard_header_11", "length_header_12", "printable_characters_ratio_header_12",
        "non_printable_characters_ratio_header_12", "letter_ratio_header_12", "digit_ratio_header_12",
        "symbol_ratio_header_12", "is_standard_header_12", "length_header_13",
        "printable_characters_ratio_header_13", "non_printable_characters_ratio_header_13",
        "letter_ratio_header_13", "digit_ratio_header_13", "symbol_ratio_header_13",
        "is_standard_header_13", "length_header_14", "printable_characters_ratio_header_14",
        "non_printable_characters_ratio_header_14", "letter_ratio_header_14", "digit_ratio_header_14",
        "symbol_ratio_header_14", "is_standard_header_14", "length_header_15",
        "printable_characters_ratio_header_15", "non_printable_characters_ratio_header_15",
        "letter_ratio_header_15", "digit_ratio_header_15", "symbol_ratio_header_15",
        "is_standard_header_15", "length_header_16", "printable_characters_ratio_header_16",
        "non_printable_characters_ratio_header_16", "letter_ratio_header_16", "digit_ratio_header_16",
        "symbol_ratio_header_16", "is_standard_header_16", "length_header_17",
        "printable_characters_ratio_header_17", "non_printable_characters_ratio_header_17",
        "letter_ratio_header_17", "digit_ratio_header_17", "symbol_ratio_header_17",
        "is_standard_header_17", "length_header_18", "printable_characters_ratio_header_18",
        "non_printable_characters_ratio_header_18", "letter_ratio_header_18", "digit_ratio_header_18",
        "symbol_ratio_header_18", "is_standard_header_18", "length_header_19",
        "printable_characters_ratio_header_19", "non_printable_characters_ratio_header_19",
        "letter_ratio_header_19", "digit_ratio_header_19", "symbol_ratio_header_19",
        "is_standard_header_19", "length_header_20", "printable_characters_ratio_header_20",
        "non_printable_characters_ratio_header_20", "letter_ratio_header_20", "digit_ratio_header_20",
        "symbol_ratio_header_20", "is_standard_header_20", "length_header_21",
        "printable_characters_ratio_header_21", "non_printable_characters_ratio_header_21",
        "letter_ratio_header_21", "digit_ratio_header_21", "symbol_ratio_header_21",
        "is_standard_header_21", "length_header_22", "printable_characters_ratio_header_22",
        "non_printable_characters_ratio_header_22", "letter_ratio_header_22", "digit_ratio_header_22",
        "symbol_ratio_header_22", "is_standard_header_22", "length_header_23",
        "printable_characters_ratio_header_23", "non_printable_characters_ratio_header_23",
        "letter_ratio_header_23", "digit_ratio_header_23", "symbol_ratio_header_23",
        "is_standard_header_23", "length_header_24", "printable_characters_ratio_header_24",
        "non_printable_characters_ratio_header_24", "letter_ratio_header_24", "digit_ratio_header_24",
        "symbol_ratio_header_24", "is_standard_header_24", "length_header_25",
        "printable_characters_ratio_header_25", "non_printable_characters_ratio_header_25",
        "letter_ratio_header_25", "digit_ratio_header_25", "symbol_ratio_header_25",
        "is_standard_header_25", "length_header_26", "printable_characters_ratio_header_26",
        "non_printable_characters_ratio_header_26", "letter_ratio_header_26", "digit_ratio_header_26",
        "symbol_ratio_header_26", "is_standard_header_26", "length_header_27",
        "printable_characters_ratio_header_27", "non_printable_characters_ratio_header_27",
        "letter_ratio_header_27", "digit_ratio_header_27", "symbol_ratio_header_27",
        "is_standard_header_27", "length_header_28", "printable_characters_ratio_header_28",
        "non_printable_characters_ratio_header_28", "letter_ratio_header_28", "digit_ratio_header_28",
        "symbol_ratio_header_28", "is_standard_header_28", "length_header_29",
        "printable_characters_ratio_header_29", "non_printable_characters_ratio_header_29",
        "letter_ratio_header_29", "digit_ratio_header_29", "symbol_ratio_header_29",
        "is_standard_header_29", "length_header_30", "printable_characters_ratio_header_30",
        "non_printable_characters_ratio_header_30", "letter_ratio_header_30", "digit_ratio_header_30",
        "symbol_ratio_header_30", "is_standard_header_30", "length_header_31",
        "printable_characters_ratio_header_31", "non_printable_characters_ratio_header_31",
        "letter_ratio_header_31", "digit_ratio_header_31", "symbol_ratio_header_31",
        "is_standard_header_31", "length_header_32", "printable_characters_ratio_header_32",
        "non_printable_characters_ratio_header_32", "letter_ratio_header_32", "digit_ratio_header_32",
        "symbol_ratio_header_32", "is_standard_header_32", "length_header_33",
        "printable_characters_ratio_header_33", "non_printable_characters_ratio_header_33",
        "letter_ratio_header_33", "digit_ratio_header_33", "symbol_ratio_header_33",
        "is_standard_header_33", "is_persistent_connection", "content_type", "length_body",
        "printable_characters_ratio_body", "non_printable_characters_ratio_body", "letter_ratio_body",
        "digit_ratio_body", "symbol_ratio_body", "num_line", "num_word")

    val testDataFrame = spark.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(resourceFolder + "normal_traffic_test.csv")
      .toDF("method", "length_path", "printable_characters_ratio_path",
        "non_printable_characters_ratio_path", "letter_ratio_path", "digit_ratio_path",
        "symbol_ratio_path", "num_segment", "is_file", "file_extension", "num_parameters", "length_query",
        "printable_characters_ratio_query", "non_printable_characters_ratio_query", "letter_ratio_query",
        "digit_ratio_query", "symbol_ratio_query", "num_headers", "standard_headers_ratio",
        "non_standard_headers_ratio", "length_header_0", "printable_characters_ratio_header_0",
        "non_printable_characters_ratio_header_0", "letter_ratio_header_0", "digit_ratio_header_0",
        "symbol_ratio_header_0", "is_standard_header_0", "length_header_1",
        "printable_characters_ratio_header_1", "non_printable_characters_ratio_header_1",
        "letter_ratio_header_1", "digit_ratio_header_1", "symbol_ratio_header_1", "is_standard_header_1",
        "length_header_2", "printable_characters_ratio_header_2",
        "non_printable_characters_ratio_header_2", "letter_ratio_header_2", "digit_ratio_header_2",
        "symbol_ratio_header_2", "is_standard_header_2", "length_header_3",
        "printable_characters_ratio_header_3", "non_printable_characters_ratio_header_3",
        "letter_ratio_header_3", "digit_ratio_header_3", "symbol_ratio_header_3", "is_standard_header_3",
        "length_header_4", "printable_characters_ratio_header_4",
        "non_printable_characters_ratio_header_4", "letter_ratio_header_4", "digit_ratio_header_4",
        "symbol_ratio_header_4", "is_standard_header_4", "length_header_5",
        "printable_characters_ratio_header_5", "non_printable_characters_ratio_header_5",
        "letter_ratio_header_5", "digit_ratio_header_5", "symbol_ratio_header_5", "is_standard_header_5",
        "length_header_6", "printable_characters_ratio_header_6",
        "non_printable_characters_ratio_header_6", "letter_ratio_header_6", "digit_ratio_header_6",
        "symbol_ratio_header_6", "is_standard_header_6", "length_header_7",
        "printable_characters_ratio_header_7", "non_printable_characters_ratio_header_7",
        "letter_ratio_header_7", "digit_ratio_header_7", "symbol_ratio_header_7", "is_standard_header_7",
        "length_header_8", "printable_characters_ratio_header_8",
        "non_printable_characters_ratio_header_8", "letter_ratio_header_8", "digit_ratio_header_8",
        "symbol_ratio_header_8", "is_standard_header_8", "length_header_9",
        "printable_characters_ratio_header_9", "non_printable_characters_ratio_header_9",
        "letter_ratio_header_9", "digit_ratio_header_9", "symbol_ratio_header_9", "is_standard_header_9",
        "length_header_10", "printable_characters_ratio_header_10",
        "non_printable_characters_ratio_header_10", "letter_ratio_header_10", "digit_ratio_header_10",
        "symbol_ratio_header_10", "is_standard_header_10", "length_header_11",
        "printable_characters_ratio_header_11", "non_printable_characters_ratio_header_11",
        "letter_ratio_header_11", "digit_ratio_header_11", "symbol_ratio_header_11",
        "is_standard_header_11", "length_header_12", "printable_characters_ratio_header_12",
        "non_printable_characters_ratio_header_12", "letter_ratio_header_12", "digit_ratio_header_12",
        "symbol_ratio_header_12", "is_standard_header_12", "length_header_13",
        "printable_characters_ratio_header_13", "non_printable_characters_ratio_header_13",
        "letter_ratio_header_13", "digit_ratio_header_13", "symbol_ratio_header_13",
        "is_standard_header_13", "length_header_14", "printable_characters_ratio_header_14",
        "non_printable_characters_ratio_header_14", "letter_ratio_header_14", "digit_ratio_header_14",
        "symbol_ratio_header_14", "is_standard_header_14", "length_header_15",
        "printable_characters_ratio_header_15", "non_printable_characters_ratio_header_15",
        "letter_ratio_header_15", "digit_ratio_header_15", "symbol_ratio_header_15",
        "is_standard_header_15", "length_header_16", "printable_characters_ratio_header_16",
        "non_printable_characters_ratio_header_16", "letter_ratio_header_16", "digit_ratio_header_16",
        "symbol_ratio_header_16", "is_standard_header_16", "length_header_17",
        "printable_characters_ratio_header_17", "non_printable_characters_ratio_header_17",
        "letter_ratio_header_17", "digit_ratio_header_17", "symbol_ratio_header_17",
        "is_standard_header_17", "length_header_18", "printable_characters_ratio_header_18",
        "non_printable_characters_ratio_header_18", "letter_ratio_header_18", "digit_ratio_header_18",
        "symbol_ratio_header_18", "is_standard_header_18", "length_header_19",
        "printable_characters_ratio_header_19", "non_printable_characters_ratio_header_19",
        "letter_ratio_header_19", "digit_ratio_header_19", "symbol_ratio_header_19",
        "is_standard_header_19", "length_header_20", "printable_characters_ratio_header_20",
        "non_printable_characters_ratio_header_20", "letter_ratio_header_20", "digit_ratio_header_20",
        "symbol_ratio_header_20", "is_standard_header_20", "length_header_21",
        "printable_characters_ratio_header_21", "non_printable_characters_ratio_header_21",
        "letter_ratio_header_21", "digit_ratio_header_21", "symbol_ratio_header_21",
        "is_standard_header_21", "length_header_22", "printable_characters_ratio_header_22",
        "non_printable_characters_ratio_header_22", "letter_ratio_header_22", "digit_ratio_header_22",
        "symbol_ratio_header_22", "is_standard_header_22", "length_header_23",
        "printable_characters_ratio_header_23", "non_printable_characters_ratio_header_23",
        "letter_ratio_header_23", "digit_ratio_header_23", "symbol_ratio_header_23",
        "is_standard_header_23", "length_header_24", "printable_characters_ratio_header_24",
        "non_printable_characters_ratio_header_24", "letter_ratio_header_24", "digit_ratio_header_24",
        "symbol_ratio_header_24", "is_standard_header_24", "length_header_25",
        "printable_characters_ratio_header_25", "non_printable_characters_ratio_header_25",
        "letter_ratio_header_25", "digit_ratio_header_25", "symbol_ratio_header_25",
        "is_standard_header_25", "length_header_26", "printable_characters_ratio_header_26",
        "non_printable_characters_ratio_header_26", "letter_ratio_header_26", "digit_ratio_header_26",
        "symbol_ratio_header_26", "is_standard_header_26", "length_header_27",
        "printable_characters_ratio_header_27", "non_printable_characters_ratio_header_27",
        "letter_ratio_header_27", "digit_ratio_header_27", "symbol_ratio_header_27",
        "is_standard_header_27", "length_header_28", "printable_characters_ratio_header_28",
        "non_printable_characters_ratio_header_28", "letter_ratio_header_28", "digit_ratio_header_28",
        "symbol_ratio_header_28", "is_standard_header_28", "length_header_29",
        "printable_characters_ratio_header_29", "non_printable_characters_ratio_header_29",
        "letter_ratio_header_29", "digit_ratio_header_29", "symbol_ratio_header_29",
        "is_standard_header_29", "length_header_30", "printable_characters_ratio_header_30",
        "non_printable_characters_ratio_header_30", "letter_ratio_header_30", "digit_ratio_header_30",
        "symbol_ratio_header_30", "is_standard_header_30", "length_header_31",
        "printable_characters_ratio_header_31", "non_printable_characters_ratio_header_31",
        "letter_ratio_header_31", "digit_ratio_header_31", "symbol_ratio_header_31",
        "is_standard_header_31", "length_header_32", "printable_characters_ratio_header_32",
        "non_printable_characters_ratio_header_32", "letter_ratio_header_32", "digit_ratio_header_32",
        "symbol_ratio_header_32", "is_standard_header_32", "length_header_33",
        "printable_characters_ratio_header_33", "non_printable_characters_ratio_header_33",
        "letter_ratio_header_33", "digit_ratio_header_33", "symbol_ratio_header_33",
        "is_standard_header_33", "is_persistent_connection", "content_type", "length_body",
        "printable_characters_ratio_body", "non_printable_characters_ratio_body", "letter_ratio_body",
        "digit_ratio_body", "symbol_ratio_body", "num_line", "num_word")

    //    clusteringTake(trainingDataFrame)
    intrusionDetector(spark, trainingDataFrame, testDataFrame)

    spark.stop
  }

  def intrusionDetector(spark: SparkSession,
                        trainingDataFrame: DataFrame,
                        testDataFrame: DataFrame): Unit = {
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
    val anomalies = clustered.filter { row =>
      val cluster = row.getAs[Int]("cluster")
      val vec = row.getAs[Vector]("scaledFeatureVector")
      Vectors.sqdist(centroids(cluster), vec) >= threshold
    }.select(originalCols.head, originalCols.tail: _*)

    println(anomalies.first())
  }

  def clusteringTake(data: DataFrame): Unit = {
    (60 to 270 by 30)
      .map(k => (k, clusteringScore(data, k)))
      .foreach(println)
  }

  def clusteringScore(data: DataFrame, k: Int): Double = {
    val model = fitPipeline(data, k)
    val kMeansModel = model.stages.last.asInstanceOf[KMeansModel]
    kMeansModel.computeCost(model.transform(data)) / data.count()
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

    val pipeline = new Pipeline().setStages(Array(methodEncoder, fileExtensionEncoder, contentTypeEncoder, assembler, scaler, kMeans))
    pipeline.fit(data)
  }

  def oneHotPipeline(column: String): (Pipeline, String) = {
    val indexer = new StringIndexer()
      .setInputCol(column)
      .setOutputCol(column + "_index")

    val encoder = new OneHotEncoder()
      .setInputCol(column + "_index")
      .setOutputCol(column + "_vector")

    val pipeline = new Pipeline().setStages(Array(indexer, encoder))
    (pipeline, column + "_vector")
  }
}
