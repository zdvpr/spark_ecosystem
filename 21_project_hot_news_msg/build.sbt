import scala.Console.in

// ver.2
lazy val _version: String = scala.io.Source
  .fromFile("VERSION")
  .getLines
  .toList.head.takeWhile(_ != ';').trim

lazy val mainSettings = Seq(
  name := "hot-news-msg",
  version := _version,
  organization := "com.example",
  scalaVersion := "2.12.12"
//  scalaVersion := "2.13.4"
)
lazy val sparkVersion = "3.2.0"
lazy val kafkaVersion = "2.8.0"

lazy val parser = (project in file(".")).
  settings(mainSettings: _*).
  settings {
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-csv" % "1.8",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "org.json4s" %% "json4s-jackson" % "3.6.6",
      "org.apache.kafka" % "kafka-clients" % kafkaVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0",
      "com.typesafe"      % "config"      % "1.4.0",
      "org.apache.spark" %% "spark-sql"   % sparkVersion % Provided,
      "org.apache.spark" %% "spark-mllib" % sparkVersion % Provided,
      "org.apache.spark"  % "spark-sql-kafka-0-10_2.12" % sparkVersion
    )
  }

assemblyMergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf")       => MergeStrategy.discard
  case m if m.toLowerCase.matches("meta-inf.*\\.sf$")   => MergeStrategy.discard
  case "reference.conf"                                 => MergeStrategy.concat
  case x: String if x.contains("UnusedStubClass.class") => MergeStrategy.first
  case _                                                => MergeStrategy.first
}
// end ver.2


//lazy val _version: String = scala.io.Source
//  .fromFile("VERSION")
//  .getLines
//  .toList.head.takeWhile(_ != ';').trim


//ver. 1
//lazy val mainSettings = Seq(
//  name := "kafka-app",
////  version := _version,
//  version := "0.1",
//  organization := "com.example",
//  scalaVersion := "2.13.4"
//)
//lazy val parser = (project in file(".")).
//  settings(mainSettings: _*).
//  settings {
//    libraryDependencies ++= Seq(
//      "org.apache.commons" % "commons-csv" % "1.8",
//      "ch.qos.logback" % "logback-classic" % "1.2.3",
//      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
//      "org.json4s" %% "json4s-jackson" % "3.6.6",
//      "org.apache.kafka" % "kafka-clients" % "2.6.0",
//      "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0",
//    )
//  }
// end ver. 1

//name := "otus-hadoop-homework"
//
//version := "0.1"

//scalaVersion := "2.12.10"
//
//val sparkVersion = "3.1.0"
//val vegasVersion = "0.3.11"
//val postgresVersion = "42.2.2"
//val scalaTestVersion = "3.2.1"
//val flinkVersion = "1.12.1"
//val circeVersion = "0.11.1"
//
////resolvers ++= Seq(
////  "bintray-spark-packages" at "https://dl.bintray.com/spark-packages/maven",
////  "Typesafe Simple Repository" at "https://repo.typesafe.com/typesafe/simple/maven-releases",
////  "MavenRepository" at "https://mvnrepository.com"
////)
//
//libraryDependencies ++= Seq(
//  "org.apache.logging.log4j" % "log4j-api" % "2.4.1",
//  "org.apache.logging.log4j" % "log4j-core" % "2.4.1",
//
//
//  "org.apache.kafka" % "kafka-clients" % "2.6.0",
//  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.0",
//  "ch.qos.logback" % "logback-classic" % "1.2.3",
//
//
//  "org.apache.commons" % "commons-csv" % "1.8",
//  "org.json4s" % "json4s-jackson" % "4.0.0"
//)
//
