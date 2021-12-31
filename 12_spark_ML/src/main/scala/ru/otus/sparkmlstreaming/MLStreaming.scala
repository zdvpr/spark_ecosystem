package ru.otus.sparkmlstreaming

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._

import java.util.Properties

object MLStreaming {
  def main(args: Array[String]): Unit = {
    // Проверяем аргументы вызова
    if (args.length != 8) {
      System.err.println(
        "Usage: MLStreaming <path-to-model> <input-bootstrap-servers> <prediction-bootstrap-servers> <groupId> <input-topic> <prediction-topic> <data-path> <data-fileName>"
      )
      System.exit(-1)
    }
    val Array( path2model, inputBrokers, predictionBrokers, groupId, inputTopic, predictionTopic, dataPath, dataFileName) = args

//    val path2model = "/Users/ruassd/trn/Scala/ecosystem/0_ДЗ/spark_ecosystem/12_spark_ML/model"
//    val inputTopic = "input"
//    val groupId = "test-consumer-group"
//    val inputBrokers = "localhost:9092"
//    val predictionBrokers = "localhost:9092"
//    val predictionTopic = "prediction"
//    val dataPath = "/Users/ruassd/trn/Scala/ecosystem/0_ДЗ/spark_ecosystem/12_spark_ML/data"
//    val dataFileName = "IRIS.csv"

    // Создаем модель
    val model = IrisFIsherModel.createModel(dataPath,dataFileName, path2model)
    // Загружаем модель (актуально при повторных запусках, когда обучение занимает много времени)
//    val model = PipelineModel.load(path2model)

    // Создаём Streaming Context и получаем Spark Context
    val sparkConf        = new SparkConf().setAppName("MLStreaming")
    val streamingContext = new StreamingContext(sparkConf, Seconds(1))
    val sparkContext     = streamingContext.sparkContext

    // Создаём свойства Producer'а для вывода в выходную тему Kafka (тема с расчётом)
    val props: Properties = new Properties()
    props.put("bootstrap.servers", predictionBrokers)

    // Создаём Kafka Sink (Producer)
    val kafkaSink = sparkContext.broadcast(KafkaSink(props))

    // Параметры подключения к Kafka для чтения
    val kafkaParams = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG        -> inputBrokers,
      ConsumerConfig.GROUP_ID_CONFIG                 -> groupId,
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG   -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer]
    )

    // Подписываемся на входную тему Kafka (тема с данными)
    val inputTopicSet = Set(inputTopic)
    val messages = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](inputTopicSet, kafkaParams)
    )

    // Разбиваем входную строку на элементы
    val lines = messages
      .map(_.value)
      .map(_.replace("\"", "").split(","))

    // Обрабатываем каждый входной набор
    lines.foreachRDD { rdd =>
      // Get the singleton instance of SparkSession
      val spark = SparkSessionSingleton.getInstance(rdd.sparkContext.getConf)
      import spark.implicits._

      // Преобразовываем RDD в DataFrame
      val data = rdd
        .toDF("input")
        .withColumn("sepal_length", $"input" (0).cast(DoubleType))
        .withColumn("sepal_width", $"input" (1).cast(DoubleType))
        .withColumn("petal_length", $"input" (2).cast(DoubleType))
        .withColumn("petal_width", $"input" (3).cast(DoubleType))
        .drop("input")

      // Если получили непустой набор данных, передаем входные данные в модель, вычисляем и выводим ID клиента и результат
      if (data.count > 0) {
        val prediction = model.transform(data)
        prediction
          .select("sepal_length", "sepal_width", "petal_length", "petal_width", "predictedLabel")
          .foreach { row => kafkaSink.value.send(predictionTopic, s"${row(0)},${row(1)},${row(2)},${row(3)},${row(4)}") }
      }
    }

    streamingContext.start()
    streamingContext.awaitTermination()
  }

  /** Lazily instantiated singleton instance of SparkSession */
  object SparkSessionSingleton {
    @transient private var instance: SparkSession = _
    def getInstance(sparkConf: SparkConf): SparkSession = {
      if (instance == null) {
        instance = SparkSession.builder
          .config(sparkConf)
          .getOrCreate()
      }
      instance
    }
  }
}
