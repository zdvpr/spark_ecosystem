package SparkDataAPI_DZ

import SparkDataAPI_DZ.DataApi_RDD.{readParquet, getPopularTime}
import org.apache.spark.sql.SparkSession
import org.scalatest.flatspec.AnyFlatSpec

class DZTestSimple extends AnyFlatSpec {

  it should "upload and process data" in {
    implicit val spark = SparkSession
      .builder()
      .config("spark.master", "local")
      .appName("Test №1 for SparkDataAPI DZ")
      .getOrCreate()

    val taxiFactsDF2 = readParquet("src/main/resources/data/yellow_taxi_jan_25_2018")(spark)
    val actualDistribution = getPopularTime(taxiFactsDF2)
      .collect()
      .head

    assert(actualDistribution._1 == "09:51:56")
    assert(actualDistribution._2 == 18)

  }

}
