package HotNewsMsg

 import org.apache.kafka.clients.producer.KafkaProducer
 import org.apache.kafka.clients.producer.ProducerRecord
 import org.apache.kafka.common.serialization.StringSerializer
 import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
 import org.apache.spark.sql.functions.concat_ws

 import java.util.Properties
 import java.io.Reader
 import scala.io.Source
 import org.json4s.jackson.JsonMethods.{compact, render}
 import org.json4s.JsonDSL._
 import org.apache.spark.sql.SparkSession

 import scala.jdk.CollectionConverters._

object NewsProducer {

//  case class News(source: String,
//                  title: String,
//                  text: String,
//                  publication_date: String,
//                  rubric: String,
//                  subrubric: String,
//                  tags: String)

  def getNewsListFromResources(inputFile: String) = {

//    def source: Reader = Source.fromResource(inputFile).reader()
    def source: Reader = Source.fromFile(inputFile).reader()

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

  def sendMsg(topic:String, props: Properties) {
//    val inputFile = "news_short.txt"
    val inputFile = "/Users/ruassd/trn/Scala/ecosystem/0_ДЗ/spark_ecosystem/21_project/src/main/resources/news_short.txt"

    val producer = new KafkaProducer(props, new StringSerializer, new StringSerializer)
    val newsList = getNewsListFromResources(inputFile)
//    val lines = Source.fromResource(inputFile).getLines()

    newsList.foreach {
      news =>
//        val jsonMsg = ("source" -> news.source) ~
//        ("title" -> news.title) ~
//        ("text" -> news.text) ~
//        ("publication_date" -> news.publication_date) ~
//        ("rubric" -> news.rubric) ~
//        ("subrubric" -> news.subrubric) ~
//        ("tags" -> news.tags)
//      val msg = compact(render(jsonMsg))
        val msg = s"${news.source},${news.title},${news.text},${news.publication_date},${news.rubric},${news.subrubric},${news.tags}"
        println(s"\n>>> $msg")
      producer.send(new ProducerRecord(topic, null, msg))
    }

    println("\n>>>messages sended\n")
    producer.close()
  }

  def sendMsgStreaming(spark: SparkSession, topic:String) {
    val inputFile = "/Users/ruassd/trn/Scala/ecosystem/0_ДЗ/spark_ecosystem/21_project/src/main/resources/news_short.txt"

    import spark.implicits._

//    val newsDF = spark.read.load("/Users/ruassd/trn/Scala/ecosystem/0_ДЗ/spark_ecosystem/21_project/src/main/resources/news_short.txt")
//    val newsDF = spark.read
//      .option("header","true")
//      .csv(inputFile)

//    Работая со SparkStreaming, чтобы писать через writeStream сначала нужно читать через readStream и подредактровать код ниже для текстового файла
//    val fileStream = StructuredStreamingKafkaDSE.getClass.getResourceAsStream("/tweets-1.json")
//    val jsonSampleString = Source.fromInputStream(fileStream).getLines().next()
//    val jsonSampleDS = spark.createDataset(List(jsonSampleString))
//    val jsonSample = spark.read.json(jsonSampleDS)
//    val schema = jsonSample.schema
    val newsDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
      .option("subscribe", "inputFile")
      .load()

    // Выводим результат
    val query = newsDF
//      .select(concat_ws(",", $"sepal_length", $"sepal_width", $"petal_length", $"petal_width", $"predictedLabel").as("value"))
      .select(concat_ws(",", $"source",$"title",$"text",$"publication_date",$"rubric",$"subrubric",$"tags").as("value"))
      .writeStream
      .option("checkpointLocation", "/Users/ruassd/trn/Scala/ecosystem/0_ДЗ/spark_ecosystem/21_project/data/checkpoint")
      .outputMode("append")
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
      .option("topic", topic)
      .start()

//    query.awaitTermination()


    println("\n>>>messages sended\n")

  }

}
