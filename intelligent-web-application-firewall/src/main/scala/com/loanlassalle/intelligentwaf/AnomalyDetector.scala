package com.loanlassalle.intelligentwaf

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.util.Random

class AnomalyDetector(val sparkSession: SparkSession) {

  def clusteringTake(data: DataFrame): Unit = {
    (60 to 270 by 30).map(k => (k, clusteringScore(data, k)))
      .foreach(println)
  }

  def clusteringScore(dataFrame: DataFrame, k: Int): Double = {
    val (methodStringIndexer, methodIndexed) =
      stringIndexerPipeline("method")
    val (fileExtensionStringIndexer, fileExtensionIndexed) =
      stringIndexerPipeline("file_extension")
    val (contentTypeStringIndexer, contentTypeIndexed) =
      stringIndexerPipeline("content_type")

    val oneHotEncoderEstimator = new OneHotEncoderEstimator()
      .setInputCols(Array(methodIndexed, fileExtensionIndexed, contentTypeIndexed))
      .setOutputCols(Array("method_vector", "file_extension_vector", "content_type_vector"))

    val assemblerCols = Set(dataFrame.columns: _*) --
      Seq("method", "file_extension", "content_type") ++
      Seq("method_vector", "file_extension_vector", "content_type_vector")

    val vectorAssembler = new VectorAssembler()
      .setInputCols(assemblerCols.toArray)
      .setOutputCol("feature_vector")

    val pipeline = new Pipeline().setStages(Array(methodStringIndexer,
      fileExtensionStringIndexer,
      contentTypeStringIndexer,
      oneHotEncoderEstimator,
      vectorAssembler))

    val pipelineModel = pipeline.fit(dataFrame)
    val normalizedDataFrame = pipelineModel.transform(dataFrame)

    val standardScaler = new StandardScaler()
      .setInputCol("feature_vector")
      .setOutputCol("scaled_feature_vector")

    val standardScalerModel = standardScaler.fit(normalizedDataFrame)
    val scaledDataFrame = standardScalerModel.transform(normalizedDataFrame)

    val kMeans = new KMeans()
      .setSeed(Random.nextLong())
      .setK(k)
      .setMaxIter(40)
      .setTol(1.0e-5)
      .setPredictionCol("prediction")
      .setFeaturesCol("scaled_feature_vector")

    val kMeansModel = kMeans.fit(scaledDataFrame)
    val predictedDataFrame = kMeansModel.transform(scaledDataFrame)

    kMeansModel.computeCost(predictedDataFrame) / predictedDataFrame.count()
  }

  def train(dataFrame: DataFrame): (KMeansModel, Double) = {
    import sparkSession.implicits._

    val (methodStringIndexer, methodIndexed) =
      stringIndexerPipeline("method")
    val (fileExtensionStringIndexer, fileExtensionIndexed) =
      stringIndexerPipeline("file_extension")
    val (contentTypeStringIndexer, contentTypeIndexed) =
      stringIndexerPipeline("content_type")

    val oneHotEncoderEstimator = new OneHotEncoderEstimator()
      .setInputCols(Array(methodIndexed, fileExtensionIndexed, contentTypeIndexed))
      .setOutputCols(Array("method_vector", "file_extension_vector", "content_type_vector"))

    val assemblerCols = Set(dataFrame.columns: _*) --
      Seq("method", "file_extension", "content_type") ++
      Seq("method_vector", "file_extension_vector", "content_type_vector")

    val vectorAssembler = new VectorAssembler()
      .setInputCols(assemblerCols.toArray)
      .setOutputCol("feature_vector")

    val pipeline = new Pipeline().setStages(Array(methodStringIndexer,
      fileExtensionStringIndexer,
      contentTypeStringIndexer,
      oneHotEncoderEstimator,
      vectorAssembler))

    val pipelineModel = pipeline.fit(dataFrame)
    val normalizedDataFrame = pipelineModel.transform(dataFrame)

    val standardScaler = new StandardScaler()
      .setInputCol("feature_vector")
      .setOutputCol("scaled_feature_vector")

    val standardScalerModel = standardScaler.fit(normalizedDataFrame)
    val scaledDataFrame = standardScalerModel.transform(normalizedDataFrame)

    val kMeans = new KMeans()
      .setSeed(Random.nextLong())
      .setK(210)
      .setMaxIter(40)
      .setTol(1.0e-5)
      .setPredictionCol("prediction")
      .setFeaturesCol("scaled_feature_vector")

    val kMeansModel = kMeans.fit(scaledDataFrame)
    val predictedDataFrame = kMeansModel.transform(scaledDataFrame)

    val distances = predictedDataFrame.map { row =>
      val prediction = row.getAs[Int]("prediction")
      val scaledFeatureVector = row.getAs[Vector]("scaled_feature_vector")
      Vectors.sqdist(kMeansModel.clusterCenters(prediction), scaledFeatureVector)
    }

    val threshold = distances.take(50).last
    kMeansModel -> threshold
  }

  def stringIndexerPipeline(inputCol: String, handleInvalid: String = "skip"):
  (StringIndexer, String) = {
    val outputCol = inputCol + "_indexed"
    val indexer = new StringIndexer()
      .setInputCol(inputCol)
      .setOutputCol(outputCol)
      .setHandleInvalid(handleInvalid)

    indexer -> outputCol
  }

  def test(kMeansModel: KMeansModel, dataFrame: DataFrame, threshold: Double): DataFrame = {

    val (methodStringIndexer, methodIndexed) =
      stringIndexerPipeline("method")
    val (fileExtensionStringIndexer, fileExtensionIndexed) =
      stringIndexerPipeline("file_extension")
    val (contentTypeStringIndexer, contentTypeIndexed) =
      stringIndexerPipeline("content_type")

    val oneHotEncoderEstimator = new OneHotEncoderEstimator()
      .setInputCols(Array(methodIndexed, fileExtensionIndexed, contentTypeIndexed))
      .setOutputCols(Array("method_vector", "file_extension_vector", "content_type_vector"))

    val assemblerCols = Set(dataFrame.columns: _*) --
      Seq("method", "file_extension", "content_type") ++
      Seq("method_vector", "file_extension_vector", "content_type_vector")

    val vectorAssembler = new VectorAssembler()
      .setInputCols(assemblerCols.toArray)
      .setOutputCol("feature_vector")

    val pipeline = new Pipeline().setStages(Array(methodStringIndexer,
      fileExtensionStringIndexer,
      contentTypeStringIndexer,
      oneHotEncoderEstimator,
      vectorAssembler))

    val pipelineModel = pipeline.fit(dataFrame)
    val normalizedDataFrame = pipelineModel.transform(dataFrame)

    val standardScaler = new StandardScaler()
      .setInputCol("feature_vector")
      .setOutputCol("scaled_feature_vector")

    val standardScalerModel = standardScaler.fit(normalizedDataFrame)
    val scaledDataFrame = standardScalerModel.transform(normalizedDataFrame)

    val kMeans = new KMeans()
      .setSeed(Random.nextLong())
      .setK(210)
      .setMaxIter(40)
      .setTol(1.0e-5)
      .setPredictionCol("prediction")
      .setFeaturesCol("scaled_feature_vector")

    val kMeansModel = kMeans.fit(scaledDataFrame)
    val predictedDataFrame = kMeansModel.transform(scaledDataFrame)

    val anomalies = predictedDataFrame.filter { row =>
      val prediction = row.getAs[Int]("prediction")
      val scaledFeatureVector = row.getAs[Vector]("scaled_feature_vector")
      Vectors.sqdist(kMeansModel.clusterCenters(prediction), scaledFeatureVector) >= threshold
    }

    anomalies
  }
}

object AnomalyDetector {
  def saveData(path: String, dataFrame: DataFrame): Unit = dataFrame.write.parquet(path + ".parquet")
}
