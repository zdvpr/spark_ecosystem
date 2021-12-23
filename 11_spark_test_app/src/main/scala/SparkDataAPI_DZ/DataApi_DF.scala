package SparkDataAPI_DZ

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{broadcast, col, date_trunc}

object DataApi_DF {

  case class PopularZonesDF(Borough: String, Count: Int)

  def readParquetDF(path: String)(implicit sparkSession: SparkSession) = sparkSession.read.load(path)

  def readCSV(path: String)(implicit sparkSession: SparkSession) = {
     sparkSession.read
      .option("header", "true")
      .csv(path)
  }

  def getPopularZones (taxiFactsDF: DataFrame,taxiZoneDF: DataFrame) = {
     taxiFactsDF
      .join(broadcast(taxiZoneDF), col("DOLocationID") === col("LocationID"), "left")
      .groupBy(col("Borough").as("Borough"))
      .count()
      .orderBy(col("count").desc)
  }

 def writeParquetDF(popularZonesDF: DataFrame, path: String) = popularZonesDF.write.parquet(path)


  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession
      .builder()
      .appName("DataApi_DF")
      .config("spark.master", "local")
      .getOrCreate()

    val taxiFactsDF = readParquetDF("src/main/resources/data/yellow_taxi_jan_25_2018")(sparkSession)
    taxiFactsDF.printSchema()

    val taxiZoneDF = readCSV("src/main/resources/data/taxi_zones.csv")(sparkSession)
    val popularZonesDF = getPopularZones(taxiFactsDF, taxiZoneDF)
    popularZonesDF.show(30)

    writeParquetDF(popularZonesDF, "trip_zones_by_reduce_popularity.parquet")

    sparkSession.stop()

  }
}
