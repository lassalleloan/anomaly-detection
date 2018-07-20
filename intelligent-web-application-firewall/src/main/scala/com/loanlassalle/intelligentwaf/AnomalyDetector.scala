package com.loanlassalle.intelligentwaf

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit, TrainValidationSplitModel}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.sql
import org.apache.spark.sql._

import scala.util.Random

/**
  * Used to detect anomalies in sequence of raw HTTP requests
  */
object AnomalyDetector extends Serializable {

  val SparkSession: SparkSession = sql.SparkSession.builder
    .master("local[*]")
    .appName("Spark Intelligent WAF")
    .getOrCreate

  /**
    * Pre-process a dataFrame to obtain scaled features
    *
    * @param path        path of CSV file
    * @param columnNames column names of CSV file rows
    * @return dataFrame with scaled features
    */
  def preProcessing(path: String, columnNames: String*): DataFrame = {

    // Gets data in CSV file
    val dataFrame = SparkSession.read
      .option("inferSchema", value = true)
      .option("header", value = false)
      .csv(path)
      .toDF(columnNames: _*)

    preProcessing(dataFrame)
  }

  /**
    * Pre-process a dataFrame to obtain scaled features
    *
    * @param dataFrame data to pre-process
    * @return dataFrame with scaled features
    */
  def preProcessing(dataFrame: DataFrame): DataFrame = {

    // Encodes a string column of labels to a column of label indices
    val (methodIndex, methodIndexer) = stringIndexer("method")
    val (fileExtensionIndex, fileExtensionIndexer) = stringIndexer("file_extension")
    val (contentTypeIndex, contentTypeIndexer) = stringIndexer("content_type")

    // Maps a categorical feature, represented as a label index, to a binary vector
    val oneHotEncoderEstimator = new OneHotEncoderEstimator()
      .setInputCols(Array(methodIndex, fileExtensionIndex, contentTypeIndex))
      .setOutputCols(Array("method_vector", "file_extension_vector", "content_type_vector"))

    // Original columns, without label / string columns, but with new vector encoded cols
    val assemblerCols = Set(dataFrame.columns: _*) --
      Seq("label", "method", "file_extension", "content_type") ++
      Seq("method_vector", "file_extension_vector", "content_type_vector")

    // Combines a given list of columns into a single vector column
    val assembler = new VectorAssembler()
      .setInputCols(assemblerCols.toArray)
      .setOutputCol("features")

    // Normalizes each feature to standard deviation and / or zero mean
    val scaler = new StandardScaler()
      .setInputCol("features")
      .setOutputCol("scaled_features")

    val pipeline = new Pipeline().setStages(Array(methodIndexer,
      fileExtensionIndexer,
      contentTypeIndexer,
      oneHotEncoderEstimator,
      assembler,
      scaler))

    val scaledData = pipeline.fit(dataFrame).transform(dataFrame)

    scaledData.select("id", "label", "scaled_features")
  }

  /**
    * Gets StringIndexer for a column
    *
    * @param inputCol      input column name
    * @param handleInvalid strategy to handle invalid data
    * @return StringIndexer for a column
    */
  def stringIndexer(inputCol: String, handleInvalid: String = "skip"):
  (String, StringIndexer) = {
    val outputCol = inputCol + "_index"
    val indexer = new StringIndexer()
      .setInputCol(inputCol)
      .setOutputCol(outputCol)
      .setHandleInvalid(handleInvalid)

    outputCol -> indexer
  }

  /**
    * Gets distances between the records and the centroid
    *
    * @param model     KMeansModel of training
    * @param dataFrame data to cluster
    * @return distances between the records and the centroid
    */
  def train(model: KMeansModel, dataFrame: DataFrame): Dataset[Double] = {

    // Makes predictions
    val predictions = model.transform(dataFrame)

    // Gets threshold to predict anomalies
    import SparkSession.implicits._
    predictions.map(distanceToCentroid(model, _))
  }

  /**
    * Gets the distance between the record and the centroid
    *
    * @param model KMeansModel
    * @param row   row of a record
    * @return distance between the record and the centroid
    */
  private def distanceToCentroid(model: KMeansModel, row: Row): Double = {
    val prediction = row.getAs[Int]("prediction")
    val features = row.getAs[Vector]("scaled_features")
    Vectors.sqdist(model.clusterCenters(prediction), features)
  }

  /**
    * Evaluates KMeans model with all combinations of parameters and determine best model using
    *
    * @param model train validation split model
    */
  def showEvaluationResults(model: TrainValidationSplitModel): Unit = {

    // Gets name and value of each parameter
    val params = model.getEstimatorParamMaps.map(paramMap =>
      paramMap.toSeq.map(paramPair => paramPair.param.name -> paramPair.value))

    // Gets metric name and all validation metrics
    val metricName = model.getEvaluator.asInstanceOf[ClusteringEvaluator].getMetricName
    val metrics = model.validationMetrics

    // Computes average after each validation metrics
    val average = for (i <- metrics.indices) yield metrics.take(i + 1).sum / (i + 1).toDouble

    // Rearranges results
    val results = params.zip(metrics).zip(average).map {
      case ((paramPair, metric), avg) => (paramPair, metric, avg)
    }

    // Show results
    results.foreach(row =>
      println(f"params: {${row._1.map(param => s"${param._1}: ${param._2}").mkString(", ")}}, " +
        f"$metricName: ${row._2}%.6f, " +
        f"avg: ${row._3}%.6f"))

    // Gets best result
    val bestResult = results.maxBy(_._3)
    println(f"Best model:" +
      f"{${bestResult._1.map(param => s"${param._1}: ${param._2}").mkString(", ")}}," +
      f"$metricName: ${bestResult._2}%.6f, " +
      f"avg: ${bestResult._3}%.6f")
  }

  /**
    * Predicts anomalies with a KMeansModel and a threshold
    *
    * @param model     KMeansModel of training
    * @param threshold threshold of training
    * @param dataFrame data to predict
    * @return anomalies predicted
    */
  def test(model: KMeansModel, threshold: Double, dataFrame: DataFrame): DataFrame = {
    val predictions = model.transform(dataFrame)
    predictions.filter(distanceToCentroid(model, _) >= threshold)
  }

  /**
    * Evaluates KMeans model with all combinations of parameters and determine best model using
    *
    * @param dataFrame     data to cluster
    * @param kValues       sequence of k values of KMeans
    * @param maxIterValues sequence of maxIter values of KMeans
    * @param tolValues     sequence of tol values of KMeans
    * @return model which contains all models generated
    */
  def evaluate(dataFrame: DataFrame,
               kValues: Seq[Int] = 60 to 270 by 30,
               maxIterValues: Seq[Int] = 20 to 40 by 10,
               tolValues: Seq[Double] = Array(1.0e-4, 1.0e-5, 1.0e-6)): TrainValidationSplitModel = {
    val kMeans = new KMeans()
      .setSeed(Random.nextLong)
      .setFeaturesCol("scaled_features")

    val evaluator = new ClusteringEvaluator().setFeaturesCol("scaled_features")

    // Constructs a grid of parameters to search over
    val paramGrid = new ParamGridBuilder()
      .addGrid(kMeans.k, kValues)
      .addGrid(kMeans.maxIter, maxIterValues)
      .addGrid(kMeans.tol, tolValues)
      .build()

    val trainValidationSplit = new TrainValidationSplit()
      .setEstimator(kMeans)
      .setEvaluator(evaluator)
      .setEstimatorParamMaps(paramGrid)
      .setTrainRatio(0.8)
      .setParallelism(2)

    // Run train validation split, and choose the best set of parameters
    trainValidationSplit.fit(dataFrame)
  }

  def validate(dataFrame: DataFrame): Map[String, Double] = {
    import SparkSession.implicits._
    val predictionAndLabels = dataFrame.map { row =>
      val label = if (row.getAs[String]("label").equals("normal")) 0.0 else 1.0
      val prediction = row.getAs[Int]("prediction").toDouble
      label -> prediction
    }

    val metrics = new MulticlassMetrics(predictionAndLabels.rdd)

    Map[String, Double]("true negative" -> metrics.confusionMatrix(0, 0),
      "false negative" -> metrics.confusionMatrix(1, 0),
      "false positive" -> metrics.confusionMatrix(0, 1),
      "true positive" -> metrics.confusionMatrix(1, 1))
  }

  /**
    * Saves dataFrame to Parquet file
    *
    * @param path      path of Parquet file
    * @param dataFrame data to save
    */
  def saveParquet(path: String, dataFrame: DataFrame): Unit = dataFrame.write
    .mode(SaveMode.Overwrite)
    .parquet(path)

  /**
    * Saves model to path
    *
    * @param path  path of model saved
    * @param model model to save
    */
  def saveModel(path: String, model: KMeansModel): Unit = model.write
    .overwrite()
    .save(path)

  /**
    * Saves pipeline to path
    *
    * @param path     path of model saved
    * @param pipeline pipeline to save
    */
  def savePipeline(path: String, pipeline: Pipeline): Unit = pipeline.write
    .overwrite()
    .save(path)
}
