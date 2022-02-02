package HotNewsMsg

import java.util.Properties
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.sql.SparkSession

object main extends LazyLogging {


  def main (args: Array[String]): Unit = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:29092")

//    val spark = SparkSession.builder
//      .appName("HotNews")
//      .getOrCreate()
    val spark = null

    val topic = "news"

    logger.info("<<< Started >>>")
    NewsProducer.sendMsg(topic , props )
    //    NewsProducer.sendMsgStreaming(spark, topic )
//    NewsConsumer.getMsgJson( topic , props )
//    NewsConsumer.getMsgStreaming(spark,  topic, props )
    logger.info("<<< Finished >>>")
  }
}
