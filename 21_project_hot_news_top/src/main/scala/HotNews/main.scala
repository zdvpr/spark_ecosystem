package HotNews

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession

object main extends LazyLogging {


  def main (args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .appName("HotNews")
      .getOrCreate()

    logger.info("<<< Started >>>")
    try {
      Publisher.getHotNews(spark)
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
