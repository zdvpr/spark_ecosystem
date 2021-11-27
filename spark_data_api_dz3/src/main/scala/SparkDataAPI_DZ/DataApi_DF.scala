package SparkDataAPI_DZ

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{broadcast, col,date_trunc}

object DataApi_DF extends App {

  case class PopularZonesDF(
                         Borough: String,
                         Count: Int)

  val sparkSession = SparkSession.builder()
    .appName("DataApi_DF")
    .config("spark.master", "local")
    .getOrCreate()

  val taxiFactsDF = sparkSession.read.load("src/main/resources/data/yellow_taxi_jan_25_2018")
  taxiFactsDF.printSchema()

  val taxiZoneDF = sparkSession.read
    .option("header", "true")
    .csv("src/main/resources/data/taxi_zones.csv")

  val popularZonesDF = taxiFactsDF
    .join(broadcast(taxiZoneDF), col("DOLocationID") === col("LocationID"), "left")
    .groupBy(col("Borough").as("Borough"))
    .count()
    .orderBy(col("count").desc)

  popularZonesDF.show(30)
  popularZonesDF.write.parquet("trip_zones_by_reduce_popularity.parquet")

  sparkSession.stop()
}

