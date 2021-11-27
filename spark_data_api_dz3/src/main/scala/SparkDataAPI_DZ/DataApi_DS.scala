package SparkDataAPI_DZ

import org.apache.spark.sql.functions.{avg,  count, max, min, round, stddev}
import org.apache.spark.sql.{SparkSession}


object DataApi_DS extends App {


  case class TaxiRide(
                       VendorID: Int,
                       tpep_pickup_datetime: String,
                       tpep_dropoff_datetime: String,
                       passenger_count: Int,
                       trip_distance: Double,
                       RatecodeID: Int,
                       store_and_fwd_flag: String,
                       PULocationID: Int,
                       DOLocationID: Int,
                       payment_type: Int,
                       fare_amount: Double,
                       extra: Double,
                       mta_tax: Double,
                       tip_amount: Double,
                       tolls_amount: Double,
                       improvement_surcharge: Double,
                       total_amount: Double
                     )


  val sparkSession = SparkSession.builder()
    .appName("DataApi_DS")
    .config("spark.master", "local")
    .getOrCreate()

  import sparkSession.implicits._

  val taxiFactsDF = sparkSession.read.load("src/main/resources/data/yellow_taxi_jan_25_2018")
  val taxiFactsDS = taxiFactsDF.as[TaxiRide]

  val tripStatDS = taxiFactsDS
    .groupBy(round($"trip_distance").alias("distance_bucket"))
    .agg(count("*").name("count_trip"),avg("trip_distance").as("avg_trip"),stddev("trip_distance").as("stddev_trip"),
      min("trip_distance").as("min_trip"), max("trip_distance").as("max_trip") )
    .orderBy($"count_trip".desc)

  tripStatDS.show()

  val driver = "org.postgresql.Driver"
  val url = "jdbc:postgresql://localhost:5432/otus"
  val user = "docker"
  val password = "docker"
  val tableName = "public.stat_by_hours"

  tripStatDS
    .write
    .format("jdbc")
    .option("url", url)
    .option("dbtable", tableName)
    .option("user", user)
    .option("password", password)
    .option("driver", "org.postgresql.Driver")
    .mode("overwrite")
    .save()

  sparkSession.stop()

}

