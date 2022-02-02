package HotNews

import com.typesafe.config.ConfigFactory

import java.util.Properties
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession

object main extends LazyLogging {


  def main (args: Array[String]): Unit = {
    val config                 = ConfigFactory.load()
    val spark = SparkSession.builder
      .appName("HotNews")
//      .master("local[2]")
      .getOrCreate()

    logger.info("<<< Started >>>")
//    NewsProducer.sendMsg(topic , props )
//    NewsProducer.sendMsgStreaming(spark, topic )
//    NewsConsumer.getMsgJson( topic , props )
    NewsConsumer.getNews(spark,  config)
    spark.stop()

    logger.info("<<< Finished >>>")
  }
}
