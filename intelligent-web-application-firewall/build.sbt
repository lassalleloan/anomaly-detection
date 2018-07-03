name := "intelligent-web-application-firewall"

version := "0.1"

scalaVersion := "2.11.12"

val sparkVersion = "2.3.1"

// https://mvnrepository.com/artifact/org.apache.spark/
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion
)

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "_" + sv.binary + "-" + sparkVersion + "_" + module.revision + "." + artifact.extension
}