# Intelligent Web Application Firewall

Author: Loan Lassalle
---

An Intelligent Web Application Firewall using Spark MLlib and k-means model.

## Dataset

The dataset is downloaded from [CSIC 2010 HTTP Dataset](http://www.isi.csic.es/dataset/)

**Training Set**: The training set is composed of data labeled as **normal** and data labeled as 
**anomaly**. The data labeled as **normal** comes from the **normalTrafficTraining.txt** file and 
data labeled as **anomaly** comes from anomalyTrafficTest.txt file.

**Testing Set**: The validation set is composed of data labeled as **normal** and data labeled as 
**anomaly**. The data labeled as **normal** comes from **normalTrafficTest.txt** file and data 
labeled as **anomaly** comes from **anomalousTrafficTest.txt** file.

## Pre-processing

In order to obtain features with raw data as HTTP request, pre-processing is required. All strings 
and information non numeric need to be transformed, `RawHttpRequest` class allows it. After the 
parsing of raw HTTP requests, `toCsv` method permits to get back numerical values of each raw HTTP 
request. To allow Spark to work on it, each `RawHttpRequest` object created are saved to a CSV file. 
3 files are created: one to train the model, one to validate it and last to test it. After these 
first steps, it is possible to go in search of the best k-means model.

## Tuning model

The model used is [k-means](http://spark.apache.org/docs/latest/ml-clustering.html#k-means) and it 
is trained on **normal** dataset and **anomaly** dataset.

To obtain the best k-means model, several parameters must be tested in order to find the most 
suitable ones, an `Evaluator`, `ParamGrid` and `TrainValidationSplit` with allows it.

## Evaluation metrics

After training of the model, you can obtain some metrics that allow you to choose the good value for
threshold.

## Testing

After the definition of the model, these parameters and the threshold, you can predict if data are 
anomalies.

## Worksheets

You can use `test.sc` and `evaluate.sc` worksheets to run the application 
with smaller datasets. Worksheet allows you to make the execution of application interactive.

To run them correctly, you need to set some parameters in IntelliJ IDEA, at `Languages & Frameworks` 
section, `Scala`, `Worksheet` tab :
 * uncheck `Run worksheet in the compiler process`
 * check `Use "eclispe comptability" mode`
 
## Spark Application

The code is organized to run as a Spark Application. To compile and run, go to folder 
[Intelligent WAF](https://github.com/lassalleloan/anomaly-detection/tree/master/intelligent-web-application-firewall) and run:

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

## References

Some code tips were inspired by the following references

  * [Parsing raw HTTP Request](https://stackoverflow.com/a/31600846) by Igor Zelaya
  * [Machine Learning Library (MLlib) Guide](http://spark.apache.org/docs/latest/ml-guide.html)
