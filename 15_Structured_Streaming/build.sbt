name := "spark_ml_streaming"

version := "0.1"

//scalaVersion := "2.13.4"
scalaVersion := "2.12.12"

lazy val sparkVersion = "3.2.0"
lazy val kafkaVersion = "2.8.0"


libraryDependencies ++= Seq(
  "com.typesafe"      % "config"      % "1.4.0",
  "org.apache.spark" %% "spark-sql"   % sparkVersion % Provided,
  "org.apache.spark" %% "spark-mllib" % sparkVersion % Provided,
  "org.apache.spark"  % "spark-sql-kafka-0-10_2.12" % sparkVersion,
  "org.apache.kafka"  % "kafka-clients"             % kafkaVersion
)

//groupId = org.apache.spark
//artifactId = spark-sql-kafka-0-10_2.12
//version = 3.2.0

//lazy val sparkVersion = "3.1.1"
//lazy val kafkaVersion = "2.7.0"
//
//libraryDependencies ++= Seq(
//  "com.typesafe"      % "config"                    % "1.4.0",
//  "org.apache.spark" %% "spark-sql"                 % sparkVersion % "provided",
//  "org.apache.spark" %% "spark-mllib"               % sparkVersion % "provided",
//  "org.apache.spark"  % "spark-sql-kafka-0-10_2.12" % sparkVersion,
//  "org.apache.kafka"  % "kafka-clients"             % kafkaVersion
//)

assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")       => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")   => MergeStrategy.discard
  case "reference.conf"                                 => MergeStrategy.concat
  case x: String if x.contains("UnusedStubClass.class") => MergeStrategy.first
  case _                                                => MergeStrategy.first
}
//
//
//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
//  case x => MergeStrategy.first
//}
//
//excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
//  cp filter {x => x.data.getName.matches("sbt.*") || x.data.getName.matches(".*macros.*")}
//}