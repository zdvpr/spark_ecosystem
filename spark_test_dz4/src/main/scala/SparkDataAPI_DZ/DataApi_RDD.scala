package SparkDataAPI_DZ

import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.rdd.RDD

object DataApi_RDD {

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

  def getPopularTime(taxiFactsDF: DataFrame)(implicit sparkSession: SparkSession) = {
    import sparkSession.implicits._
    val taxiFactsDS: Dataset[TaxiRide] = taxiFactsDF.as[TaxiRide]
    val taxiFactsRDD: RDD[TaxiRide]    = taxiFactsDS.rdd

    taxiFactsRDD
      .map(l => (l.tpep_pickup_datetime.substring(11), 1))
      .reduceByKey(_ + _)
      .sortBy(_._2, false)
  }

  def writeTextFile(popularTimeRDD: RDD[(String, Int)], fileName: String) = {
    popularTimeRDD.saveAsTextFile(fileName)
  }

  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession
      .builder()
      .appName("DataApi_RDD")
      .config("spark.master", "local")
      .getOrCreate()

    val taxiFactsDF    = readParquet("src/main/resources/data/yellow_taxi_jan_25_2018")(sparkSession)
    val popularTimeRDD = getPopularTime(taxiFactsDF)(sparkSession)

    writeTextFile(popularTimeRDD, "PopularTime.txt")

    sparkSession.stop()
  }
}
