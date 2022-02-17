package HotNewsMsg

import java.util.Properties
import com.typesafe.scalalogging.LazyLogging


object main extends LazyLogging {


  def main(args: Array[String]): Unit = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:29092")
    val topic = "news_stream"

    logger.info("<<< Started >>>")
    NewsProducer.sendMsg(topic, props)
    logger.info("<<< Finished >>>")
  }
}
