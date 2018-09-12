import com.loanlassalle.intelligentwaf.IntelligentWaf.getClass
import com.loanlassalle.intelligentwaf.{AnomalyDetector, RawHttpRequest}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.clustering.KMeans

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

val k = 20
val maxIter = 60
val tol = 1.0E-6
val bestModel = new KMeans()
  .setK(k)
  .setMaxIter(maxIter)
  .setTol(tol)
  .setFeaturesCol("scaled_features")
  .fit(training)

/**
  * Gets all distances to centroids for normal distribution
  */
AnomalyDetector.saveDistancesToCentroids(s"$resourcesPath/results_distances-40.csv",
  bestModel,
  testing)

/**
  * Gets prediction tests' results based on thresholds
  */
println("Prediction tests' results based on thresholds")
val thresholds = for (decimal <- BigDecimal(0) to BigDecimal(10) by BigDecimal(0.1))
  yield
    decimal.doubleValue()
AnomalyDetector.saveTestsResults(s"$resourcesPath/results_tests-40.csv",
  bestModel,
  thresholds,
  testing)
println

/**
  * Tests the model
  */
println("Intelligent WAF on test.csv")

val threshold = 3.0
val anomalies = AnomalyDetector.test(bestModel, threshold, testing)

val totalAnomalies = testing.filter(row =>
  row.getAs[String]("label")
    .equals("anomaly"))
  .count
val actualAnomalies = anomalies.filter(row =>
  row.getAs[String]("label")
    .equals("anomaly"))
  .count

println(s"Number of anomalies in file: $totalAnomalies")
println(s"Number of anomalies detected: ${anomalies.count}")
println(s"Number of actual anomalies detected: $actualAnomalies")
println(f"Error rate of anomalies detected: ${
  math.abs(actualAnomalies - anomalies.count).toDouble / anomalies.count * 100
}%.2f%%")

anomalies.show(3)