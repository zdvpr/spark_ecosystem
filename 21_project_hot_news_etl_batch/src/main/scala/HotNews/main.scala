package HotNews


import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession

object main extends LazyLogging {


  def main (args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .appName("HotNews")
//      .master("local[2]")
      .getOrCreate()

    val busDay = 1598896680  //
    val nextBusDay = 1598810280 //

    logger.info("<<< Started >>>")
    try {
      NewsConsumer.getNews(spark,busDay,nextBusDay)
    }
    catch {
      case e: Exception => println( s": ${e.getMessage}\n\n${e.getStackTrace.mkString("\n")}")
    }
    finally {
      spark.stop()
    }
    logger.info("<<< Finished >>>")
  }
}
