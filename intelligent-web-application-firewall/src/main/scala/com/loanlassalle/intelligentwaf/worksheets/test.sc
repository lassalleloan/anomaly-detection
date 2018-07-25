import com.loanlassalle.intelligentwaf.IntelligentWaf.getClass
import com.loanlassalle.intelligentwaf.{AnomalyDetector, RawHttpRequest}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.clustering.KMeans

/**
  * Disables some types of logger message
  */
Logger.getLogger("org").setLevel(Level.ERROR)
Logger.getLogger("com").setLevel(Level.ERROR)

val resourcesPath = getClass.getResource("/csic_2010_http_dataset").getPath

/**
  * Pre-processes of raw data for anomaly detection
  */
val normalTraining = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTraining-20.txt",
  "normal")
val normalTest = RawHttpRequest.parse(s"$resourcesPath/normalTrafficTest-20.txt", "normal")
val anomalous = RawHttpRequest.parse(s"$resourcesPath/anomalousTrafficTest-20.txt", "anomaly")

println(s"Basic statistics of all dataset")
RawHttpRequest.basicStatistics(normalTraining ++ normalTest ++ anomalous)
println

RawHttpRequest.saveCsv(s"$resourcesPath/train.csv", normalTraining ++ anomalous)
RawHttpRequest.saveCsv(s"$resourcesPath/test.csv", normalTest ++ anomalous)

val columnNames = RawHttpRequest.columnNames
val training = AnomalyDetector.preProcessing(s"$resourcesPath/train.csv", columnNames: _*)
val testing = AnomalyDetector.preProcessing(s"$resourcesPath/test.csv", columnNames: _*)

val k = 18
val maxIter = 20
val tol = 1.0E-4
val bestModel = new KMeans().setK(k).setMaxIter(maxIter).setTol(tol).setFeaturesCol ("scaled_features").fit(training)

/**
  * Gets all distances to centroids for normal distribution
  */
AnomalyDetector.saveDistancesToCentroids(s"$resourcesPath/results_distances.csv", bestModel,
  testing)

/**
  * Tests the model
  */
val threshold = 3.0
val anomalies = AnomalyDetector.test(bestModel, threshold, testing)

println("Intelligent WAF on test.csv")
println(s"Number of anomalies in file: ${
  testing.filter(row =>
    row.getAs[String]("label")
      .equals("anomaly"))
    .count
}")
println(s"Number of anomalies detected: ${anomalies.count}")
println(s"Number of actual anomalies detected: ${
  anomalies.filter(row =>
    row.getAs[String]("label")
      .equals("anomaly"))
    .count
}")
anomalies.show()