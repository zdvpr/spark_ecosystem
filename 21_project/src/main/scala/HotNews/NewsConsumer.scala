package HotNews

import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

import scala.jdk.CollectionConverters._
import org.apache.kafka.common.TopicPartition
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}

import java.util.Properties
import java.time.Duration
import java.util


object NewsConsumer {

  def readStreamNews (spark: SparkSession, config: Config) = {
    val bootstrapServers  = config.getString("kafka.bootstrap.servers")
    val topic             = config.getString("kafka.topic")

    import spark.implicits._

  // Читаем входной поток
    spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("group_id","consumer1")
      .option("subscribe", topic)
      .option("startingOffset","earliest")
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String]
      .map(_.replace("\"", "").split(","))
      .map(News(_))
  }

  // Пишем датасет новостей в консоль
  def writeNewsConsole(dsNews: Dataset[News]) = {
    dsNews.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  // Задаем конфигурацию Postgres
  def postgresqlSinkOptions(config: Config): Map[String, String] =  {

    Map(
      "dbtable" -> config.getString("tableName"), // table
      "user" -> config.getString("userDB"), // Database username
      "password" -> config.getString("password"), // Password
      "driver" -> config.getString("driver"),
      "url" -> config.getString("url")
    )
  }

  // Пишем датасет новостей в Postgres
  def writeNewsPostgres(dsNews: Dataset[News], config: Config) = {
    val checkpointLocation = config.getString("checkpointLocation")

    dsNews
      .writeStream
      .option("checkpointLocation",checkpointLocation)
      .foreachBatch { (batch: Dataset[News], _: Long) =>
        batch.write
          .options(postgresqlSinkOptions(config))
          .mode(SaveMode.Append)
          .save()
      }
      .start()
      .awaitTermination()
  }

  def getNews (spark: SparkSession, config: Config) {

    val input = readStreamNews(spark, config: Config)
   // writeNewsConsole(input)
    writeNewsPostgres(input, config)

  }
}
