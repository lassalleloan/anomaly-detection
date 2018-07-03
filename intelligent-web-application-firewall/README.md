Intelligente Web Application Firewall
==============

To build a JAR:

    sbt package

To run on a cluster with Spark installed:

    spark-submit --master local \
      target/scala-2.11/intelligent-web-application-firewall_2.11-0.1.jar <input file>

To run a REPL that can reference the objects and classes defined in this project:

    spark-shell --jars target/scala-2.11/intelligent-web-application-firewall_2.11-0.1.jar --master local

The `--master local` argument means that the application will run in a single local process.  If
the cluster is running a Spark standalone cluster manager, you can replace it with
`--master spark://<master host>:<master port>`. If the cluster is running YARN, you can replace it
with `--master yarn`.

To pass configuration options on the command line, use the `--conf` option, e.g.
`--conf spark.serializer=org.apache.spark.serializer.KryoSerializer`.
