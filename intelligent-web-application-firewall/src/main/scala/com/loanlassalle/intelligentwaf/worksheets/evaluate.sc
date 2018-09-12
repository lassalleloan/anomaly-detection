import com.loanlassalle.intelligentwaf.IntelligentWaf.getClass
import com.loanlassalle.intelligentwaf.{AnomalyDetector, RawHttpRequest}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.clustering.KMeansModel

/**
  * Disables some types of logger message
  */
Logger.getLogger("org").setLevel(Level.ERROR)
Logger.getLogger("com").setLevel(Level.ERROR)

val resourcesPath = getClass.getResource("/csic_2010_http_dataset/partially").getPath

/**
  * Pre-processes of raw data for anomaly detection
  */
val normalTraining = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTraining-20.txt",
  "normal")
val normalTest = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTest-20.txt",
  "normal")
val anomalous = RawHttpRequest.parse(s"$resourcesPath/anomalousTrafficTest-20.txt",
  "anomaly")

println(s"Basic statistics of all dataset")
RawHttpRequest.basicStatistics(normalTraining ++ normalTest ++ anomalous)
println

RawHttpRequest.saveCsv(s"$resourcesPath/train-40.csv", normalTraining ++ anomalous)
RawHttpRequest.saveCsv(s"$resourcesPath/test-40.csv", normalTest ++ anomalous)

val columnNames = RawHttpRequest.columnNames
val training = AnomalyDetector.preProcessing(s"$resourcesPath/train-40.csv", columnNames: _*)
val testing = AnomalyDetector.preProcessing(s"$resourcesPath/test-40.csv", columnNames: _*)

/**
  * Tunes KMeans model with all combinations of parameters and determine the best
  * model
  * using
  */
val trainModels = AnomalyDetector.tune(training,
  10 to 20,
  20 to 60 by 5,
  Array(1.0E-4, 1.0E-5, 1.0E-6))

println("Tuning of k-Means model")
AnomalyDetector.showTuningResults(trainModels)
AnomalyDetector.saveTuningResults(s"$resourcesPath/results_tuning-40.csv", trainModels)
println

/**
  * Evaluates the model
  */
println("Evaluation of k-Means model")
val bestModel = trainModels.bestModel.asInstanceOf[KMeansModel]
val metrics = AnomalyDetector.evaluate(bestModel, testing)
AnomalyDetector.saveEvaluationResults(s"$resourcesPath/results_evaluation-40.csv", metrics)
println
