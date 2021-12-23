package KafkaDZ

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import scala.jdk.CollectionConverters._
import org.apache.kafka.common.TopicPartition

import java.util.Properties
import java.time.Duration
import java.util


object BooksConsumer {

  def getMsg(topic:String, props: Properties) {

    props.put("group.id", "consumer1")

    val consumer = new KafkaConsumer(props, new StringDeserializer, new StringDeserializer)

    consumer.subscribe(List("books").asJavaCollection)

    val partitions = consumer.partitionsFor(topic).asScala
    val topicList = new util.ArrayList[TopicPartition]()

    partitions.foreach {
      p =>
        topicList.add(new TopicPartition(p.topic(), p.partition()))
    }

    consumer.unsubscribe()
    consumer.assign(topicList)
    consumer.seekToEnd(topicList)
    val cntMessages = 5

    topicList.asScala.foreach(p => consumer.seek( p, consumer.position(p) - cntMessages) )

    consumer
      .poll(Duration.ofSeconds(1))
      .asScala
      .foreach { r => println(r.value()) }

    consumer.close()
  }
}
