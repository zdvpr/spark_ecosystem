package SparkDataAPI_DZ

import SparkDataAPI_DZ.DataApi_DS.{createStatData, readParquet}
import SparkDataAPI_DZ.DataApi_DF.{getPopularZones, readCSV, readParquetDF}
import org.apache.spark.sql.QueryTest.checkAnswer
import org.apache.spark.sql.test.SharedSparkSession
import org.apache.spark.sql.{Row, SQLContext, SQLImplicits, SparkSession}


class DZTestShared extends SharedSparkSession {


  test("Check StatTaxiData DS") {

    val taxiDF2 = readParquet("src/main/resources/data/yellow_taxi_jan_25_2018")
    val actualDistribution = createStatData(taxiDF2)

    checkAnswer(
      actualDistribution,
        Row("5",289702, 1.62, 1.04,0,5)  ::
        Row("10",25277, 7.16, 1.5,5.01,10.0)  ::
        Row("other",16914, 14.92,  4.19,10.01,66.0)  :: Nil
    )

  }

  test("Check PopularTaxiZones DF") {
    val taxiZonesDF2 = readCSV("src/main/resources/data/taxi_zones.csv")
    val taxiDF2 = readParquetDF("src/main/resources/data/yellow_taxi_jan_25_2018")

    val actualDistribution = getPopularZones(taxiDF2, taxiZonesDF2)

    checkAnswer(
      actualDistribution,
        Row("Manhattan",296527)  ::
        Row("Queens",13819) ::
        Row("Brooklyn",12672) ::
        Row("Unknown",6714) ::
        Row("Bronx",1589) ::
        Row("EWR",508) ::
        Row("Staten Island",64) :: Nil
    )

  }

}