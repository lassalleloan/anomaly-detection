# Intelligent Web Application Firewall

Author: Loan Lassalle
---

An Intelligent Web Application Firewall using Spark MLlib and k-Means model.

## Dataset

The dataset is downloaded from [CSIC 2010 HTTP Dataset](http://www.isi.csic.es/dataset/)

**Training Set**: The training set is composed of data labeled as **normal** and data labeled as **anomaly**. The data labeled as **normal** comes from the **normalTrafficTraining.txt** file and data labeled as **anomaly** comes from anomalyTrafficTest.txt file.

**Validation Set**: The validation set is composed of data labeled as **normal** and data labeled as **anomaly**. The data labeled as **normal** comes from **normalTrafficTest.txt** file and data labeled as **anomaly** comes from **anomalousTrafficTest.txt** file.

**Testing Set**: The validation set is composed of all previous data sets.

## Pre-processing

In order to obtain features with raw data as HTTP request, pre-processing is required. All strings and information non numeric need to be transformed, `RawHttpRequest` class allows it. After the parsing of raw HTTP requests, `toCsv` method permits to get back numerical values of each raw HTTP request. To allow Spark to work on it, each `RawHttpRequest` object created are saved to a CSV file. 3 files are created: one to train the model, one to validate it and last to test it. After these first steps, it is possible to go in search of the best k-Means model.

## Evaluation

To obtain the best k-Means model, several parameters must be tested in order to find the most suitable ones, an `Evaluator`, `ParamGrid` and `TrainValidationSplit` with allows it.

## Modelisation

### Anomaly Detection Model

This model is using [KMeans](http://spark.apache.org/docs/latest/ml-clustering.html#k-means) approach and it is trained on **normal** dataset and a part of **anomaly** dataset. After the model is trained, the distance to **centroids** of the dataset will be returned. It allows to define a **threshold**. During the validation and test stage, any data points that are further than the **threshold** from the **centroid** are considered as **anomalies**.

Also, a confusion matrix is computed. It permits to allows visualization of the performance of an algorithm 

## Spark Application

The code is organized to run as a Spark Application. To compile and run, go to folder [Intelligent WAF](https://github.com/lassalleloan/anomaly-detection/tree/master/intelligent-web-application-firewall) and run:

	sbt package
	spark-submit --master local \
	--class IntelligentWAF \
	target/scala-2.11/intelligent-web-application-firewall_2.11-2.3.1_0.1.jar

The `--master local` argument means that the application will run in a single local process.  If
the cluster is running a Spark standalone cluster manager, you can replace it with
`--master spark://<master host>:<master port>`. If the cluster is running YARN, you can replace it
with `--master yarn`.

To pass configuration options on the command line, use the `--conf` option, e.g.
`--conf spark.serializer=org.apache.spark.serializer.KryoSerializer`.

## Worksheets

Alternatively, you can also use `test.sc` and `evaluate.sc` worksheets to run the application 
with smaller datasets.

To run them correctly, you need to set some parameters in IntelliJ IDEA, at `Languages & Frameworks` section, `Scala`, `Worksheet` tab :
 * uncheck `Run worksheet in the compiler process`
 * check `Use "eclispe comptability" mode`

## Refences

Some code tips were inspired by the following references

  * [Parsing raw HTTP Request](https://stackoverflow.com/a/31600846) by Igor Zelaya
  * [Machine Learning Library (MLlib) Guide](http://spark.apache.org/docs/latest/ml-guide.html)
