package SparkDataAPI_DZ

import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

object DataApi_DS {

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

  def readParquet(path: String)(implicit sparkSession: SparkSession) = sparkSession.read.load(path)

  def createStatData(taxiFactsDF: DataFrame)(implicit sparkSession: SparkSession) = {
    import sparkSession.implicits._

    taxiFactsDF
      .as[TaxiRide]
      .withColumn("distance_bucket", when(col("trip_distance") <= lit(5),5)
        .when( col("trip_distance") > lit(5) && col("trip_distance") <= lit(10),10)
        .otherwise("other") )
      .groupBy(col("distance_bucket"))
      .agg(
        count("*").as("count_trip"),
        round(avg("trip_distance").as("avg_trip"),2),
        round(stddev("trip_distance").as("stddev_trip"),2),
        min("trip_distance").as("min_trip"),
        max("trip_distance").as("max_trip")
      )
      .orderBy(col("count_trip").desc)
  }

  def writeStatData(
      tripStatDS: DataFrame,
      format: String,
      url: String,
      tableName: String,
      user: String,
      password: String,
      driver: String,
      mode: String
  ) = {
    tripStatDS.write
      .format(format)
      .option("url", url)
      .option("dbtable", tableName)
      .option("user", user)
      .option("password", password)
      .option("driver", driver)
      .mode(mode)
      .save()
  }

  def main(args: Array[String]): Unit = {
    implicit val sparkSession = SparkSession
      .builder()
      .appName("DataApi_DS")
      .config("spark.master", "local")
      .getOrCreate()

    val taxiFactsDF = readParquet("src/main/resources/data/yellow_taxi_jan_25_2018")

    val tripStatDS = createStatData(taxiFactsDF)(sparkSession)
    tripStatDS.show()

    writeStatData(
      tripStatDS,
      "jdbc",
      "jdbc:postgresql://localhost:5432/otus",
      "public.stat_by_hours",
      "docker",
      "docker",
      "org.postgresql.Driver",
      "overwrite"
    )

    sparkSession.stop()
  }

}
