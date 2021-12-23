package SparkDataAPI_DZ


import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.rdd.RDD
import java.io.{FileOutputStream, PrintStream}

object DataApi_RDD extends App {

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
      .appName("DataApi_RDD")
      .config("spark.master", "local")
      .getOrCreate()

  import sparkSession.implicits._

  val taxiFactsDF = sparkSession.read.load("src/main/resources/data/yellow_taxi_jan_25_2018")
  val taxiFactsDS: Dataset[TaxiRide] = taxiFactsDF.as[TaxiRide]

  val taxiFactsRDD: RDD[TaxiRide] = taxiFactsDS.rdd

  val fos = new FileOutputStream("PopularTime.txt")
  val printer = new PrintStream(fos)


  val popularTimeRDD = taxiFactsRDD
      .map(l => (l.tpep_pickup_datetime.substring(11),1))
      .reduceByKey(_ + _)
      .sortBy(_._2, false)
      .foreach( x => (println(x._1 + " " + x._2.toString), printer.println(x._1 + " " + x._2.toString)))

  sparkSession.stop()

}

