package HotNewsMsg

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}

import java.util.Properties
import java.io.Reader
import scala.io.Source
import scala.collection.mutable
import scala.jdk.CollectionConverters._

object NewsProducer {


  def getNewsListFromFile(inputFile: String): mutable.Buffer[News] = {

    def source: Reader = Source.fromResource(inputFile).reader()

    val records = CSVParser.parse(source, CSVFormat.DEFAULT.withHeader()).getRecords.asScala

    for (record <- records) yield News(
      record.get("source")
      , record.get("title")
      , record.get("text")
      , record.get("publication_date")
      , record.get("rubric")
      , record.get("subrubric")
      , record.get("tags")
    )
  }

  def sendMsg(topic: String, props: Properties) {
    val inputFile = "news.txt"

    val producer = new KafkaProducer(props, new StringSerializer, new StringSerializer)
    val newsList = getNewsListFromFile(inputFile)
    var i = 0
    newsList.foreach {
      news =>
        i = i + 1
        val msg = s"${i.toString};&#${news.source};&#${news.title};&#${news.text};&#${news.publication_date}"
        println(s"\n>>> $msg\n")
        producer.send(new ProducerRecord(topic, null, msg))
//        Thread.sleep(10)
    }

    println("\n>>> Новости отправлены в Кафку!!!\n")
    producer.close()
  }

}
