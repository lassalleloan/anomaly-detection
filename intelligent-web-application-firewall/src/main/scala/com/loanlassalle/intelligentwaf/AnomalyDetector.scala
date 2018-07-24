package com.loanlassalle.intelligentwaf

import java.io.{File, PrintWriter}

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.clustering.{KMeans, KMeansModel}
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.{Vector, Vectors}
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit, TrainValidationSplitModel}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.sql
import org.apache.spark.sql._

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
    require(path.endsWith(".csv"), "file must be a CSV file")

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
    val inputCols = Seq("method", "file_extension", "content_type")

    require(inputCols.toSet.subsetOf(dataFrame.columns.toSet),
      s"dataFrame must contain $inputCols columns")

    // Encodes label string columns into label index columns
    val indexers = stringIndexers(inputCols)

    // Maps a categorical feature, represented as a label index, to a binary vector
    val oneHotEncoderEstimator = new OneHotEncoderEstimator()
      .setInputCols(indexers.keySet.toArray)
      .setOutputCols(inputCols.map(_ + "_vector").toArray)

    // Original columns, without id and label / string columns, but with new vector encoded cols
    val assemblerCols = Set(dataFrame.columns: _*) --
      Seq("id", "label") -- inputCols ++
      oneHotEncoderEstimator.getOutputCols

    // Combines a given list of columns into a single vector column
    val assembler = new VectorAssembler()
      .setInputCols(assemblerCols.toArray)
      .setOutputCol("features")

    // Normalizes each feature to standard deviation and / or zero mean
    val scaler = new StandardScaler()
      .setWithMean(true)
      .setInputCol(assembler.getOutputCol)
      .setOutputCol("scaled_features")

    val pipeline = new Pipeline().setStages(indexers.values.toArray ++
      Array(oneHotEncoderEstimator, assembler, scaler))

    val scaledData = pipeline.fit(dataFrame).transform(dataFrame)

    scaledData.select("id", "label", "scaled_features")
  }

  /**
    * Gets sequence of StringIndexers for columns
    *
    * @param inputCols     input column name
    * @param handleInvalid strategy to handle invalid data
    * @return map of StringIndexers for columns
    */
  def stringIndexers(inputCols: Seq[String], handleInvalid: String = "skip"):
  Map[String, StringIndexer] = {
    (for (inputCol <- inputCols) yield stringIndexer(inputCol)).toMap
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
    * Displays the tuning of KMeans model with all combinations of parameters
    *
    * @param model         KMeans model to display the tuning
    * @param onlyBestModel indicates displays only best model
    */
  def showTuningResults(model: TrainValidationSplitModel, onlyBestModel: Boolean = true): Unit = {

    // Gets results
    val results = tuningResults(model)

    // Show results
    if (!onlyBestModel) {
      results.foreach(row =>
        println(row.map(param => f"${param._1}: ${param._2}%.6f").mkString(", ")))
    }

    // Gets best result
    val bestResult = results.maxBy(row => row.filter(param => param._1.equals("avg")).map(_._2).head)
    println(f"Best model:" +
      f"${bestResult.map(param => f"${param._1}: ${param._2}%.6f").mkString(", ")}")
  }

  /**
    * Gets tuning results of KMeans model with all combinations of parameters
    *
    * @param model KMeans model to display the tuning
    * @return tuning results of KMeans model with all combinations of parameters
    */
  private def tuningResults(model: TrainValidationSplitModel): Seq[Seq[(String, Double)]] = {

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
      case ((paramPair, metric), avg) =>
        paramPair.map(param => param._1 -> (param._2 match {
          case i: Int => i
          case f: Float => f
          case d: Double => d
        })) ++
          Seq(metricName -> metric, "avg" -> avg)
    }

    results
  }

  /**
    * Saves some tuning results to a CSV file
    *
    * @param path  path of CSV file
    * @param model KMeans model to display the tuning
    */
  def saveTuningResults(path: String, model: TrainValidationSplitModel): Unit = {

    // Gets results
    val results = tuningResults(model)

    // Gets best result
    val bestResult = results.maxBy(row => row.filter(param => param._1.equals("avg")).map(_._2).head)
    val bestResultTol = bestResult.filter(param => param._1.equals("tol")).head

    val resultsWithBestTol = results.filter(row => row.contains(bestResultTol))

    // Gets list of parameters applied to the model during tuning
    val kList = resultsWithBestTol
      .flatMap(row => row.filter(param => param._1.equals("k")).map(_._2))
      .distinct
    val maxIterList = resultsWithBestTol
      .flatMap(row => row.filter(param => param._1.equals("maxIter")).map(_._2))
      .distinct

    val writer = new PrintWriter(new File(path))

    // Writes list of k values as column names
    writer.write(f",${kList.mkString(",")}\n")

    maxIterList.foreach { maxIter =>

      // Writes maxIter values as row names
      writer.write(f"$maxIter,")

      // Writes all metrics as a function of k and maxIter
      writer.write(kList.flatMap { k =>
        resultsWithBestTol
          .filter(row => row.contains(("maxIter", maxIter)) && row.contains(("k", k)))
          .map(row => row.filter(param => param._1.equals("avg")).map(_._2).head)
      }.mkString(","))

      writer.write(f"\n")
    }

    writer.close()
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
    * Tunes KMeans model with all combinations of parameters and determine best model
    *
    * @param dataFrame     data to tune
    * @param kValues       sequence of k values of KMeans
    * @param maxIterValues sequence of maxIter values of KMeans
    * @param tolValues     sequence of tol values of KMeans
    * @return best KMeans model
    */
  def tune(dataFrame: DataFrame,
           kValues: Seq[Int] = Array(2),
           maxIterValues: Seq[Int] = Array(20),
           tolValues: Seq[Double] = Array(1.0e-4)): TrainValidationSplitModel = {
    val kMeans = new KMeans()
      .setFeaturesCol("scaled_features")

    val evaluator = new ClusteringEvaluator().setFeaturesCol(kMeans.getFeaturesCol)

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

  /**
    * Evaluates metrics of predictions with a binary classification metrics
    *
    * @param model     KMeansModel of training
    * @param dataFrame data to evaluate
    * @return a binary classification metrics
    */
  def evaluate(model: KMeansModel, dataFrame: DataFrame): BinaryClassificationMetrics = {
    val predictions = model.transform(dataFrame)

    import SparkSession.implicits._
    val predictionsAndLabels = predictions.map { row =>
      val label = row.getAs[String]("label").equals("normal").compareTo(false).toDouble
      val prediction = row.getAs[Int]("prediction").toDouble
      prediction -> label
    }

    new BinaryClassificationMetrics(predictionsAndLabels.rdd)
  }

  /**
    * Displays results of the evaluation of metrics
    *
    * @param metrics metrics evaluated
    */
  def showEvaluationResults(metrics: BinaryClassificationMetrics): Unit = {

    // Precision by threshold
    metrics.precisionByThreshold.foreach { case (t, p) =>
      println(f"Threshold: $t%.4f, Precision: $p%.4f")
    }

    // Recall by threshold
    metrics.recallByThreshold.foreach { case (t, r) =>
      println(f"Threshold: $t%.4f, Recall: $r%.4f")
    }

    // Precision-Recall Curve
    val PRC = metrics.pr

    // F-measure
    metrics.fMeasureByThreshold.foreach { case (t, f) =>
      println(f"Threshold: $t%.4f, F-score: $f%.4f, Beta = 1")
    }

    val beta = 0.5
    metrics.fMeasureByThreshold(beta).foreach { case (t, f) =>
      println(f"Threshold: $t%.4f, F-score: $f%.4f, Beta = $beta")
    }

    // AUPRC
    val auPRC = metrics.areaUnderPR
    println(f"Area under precision-recall curve = $auPRC%.4f")

    // ROC Curve
    val roc = metrics.roc

    // AUROC
    val auROC = metrics.areaUnderROC
    println(f"Area under ROC = $auROC%.4f")
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
