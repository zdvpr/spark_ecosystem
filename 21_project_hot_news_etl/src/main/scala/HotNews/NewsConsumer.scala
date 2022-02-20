package HotNews

import com.typesafe.config.{ConfigFactory}

import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{DataStreamWriter, Trigger}
import java.sql.BatchUpdateException


object NewsConsumer {
  val config = ConfigFactory.load()
  val checkpointLocation = config.getString("checkpointLocation")
  val checkpointLocationNews = config.getString("checkpointLocationNews")


  val postgreOptions = Map(
    "user" -> config.getString("userDB"), // Database username
    "password" -> config.getString("password"), // Password
    "driver" -> config.getString("driver"),
    "url" -> config.getString("url")
  )

  val prefix = "How news!!! >>>  "

  def readStreamNews(spark: SparkSession): Dataset[News] = {
    val bootstrapServers = config.getString("kafka.bootstrap.servers")
    val topic = config.getString("kafka.topic")

    import spark.implicits._

    // Читаем входной поток
    spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("group_id", "consumer1")
      .option("subscribe", topic)
      .option("failOnDataLoss", false)
//      .option("startingOffset","latest")
      .option("startingOffset","earliest")
      .load()
      .selectExpr("CAST(value AS STRING)")
      .as[String]
      .map(_.replaceAll("[,.\"«»!?—]", "").replace("\u00A0", " ").split(";&#"))
      .map(News(_))

  }

  // Пишем датасет новостей в консоль
  def writeNewsConsole(dsNews: Dataset[News]): Unit = {
    println(s"\n\n$prefix Новости. Начало. Консоль.\n\n")
    dsNews.writeStream
      .format("console")
      .outputMode("append")
      .start()
      .awaitTermination()
  }

  // Пишем датасет новостей в файл
  def writeNewsFile(dsNews: Dataset[News]): Unit = {
    println(s"\n\n$prefix Новости. Начало. Файл.\n\n")
    dsNews.writeStream
      .format("csv")
      .outputMode("append")
      //      .trigger(processingTime="10 seconds")
      .option("checkpointLocation", checkpointLocation)
      .option("path", "output_dsNews/")
      .start()
      .awaitTermination()
  }

  // Пишем датасет новостей в Postgres
  def writeNewsPostgres(dsNews: Dataset[News]): Unit = {
    println(s"\n\n$prefix Запись новостей в БД\n\n")

    dsNews
      //      .drop(col("publiction_date"))
      .writeStream
      .option("checkpointLocation", checkpointLocationNews)
      //      .trigger(Trigger.ProcessingTime(2.seconds))
      .foreachBatch { (batch: Dataset[News], _: Long) =>
        batch.write
          .format("jdbc")
          .options(postgreOptions)
          .option("dbtable", config.getString("newsTableName"))
          .mode(SaveMode.Append)
          //          .mode(SaveMode.Overwrite)
          .save()
      }
      .start()
    //      .awaitTermination()

  }

  // Пишем датасет новостей в Postgres
  def writeNewsPostgres2(dsNews: Dataset[News]): Unit = {
    println(s"\n\n$prefix Новости. Начало. БД.\n\n")
    dsNews
      .writeStream
      .option("checkpointLocation", checkpointLocation)
      .foreachBatch { (batch: Dataset[News], _: Long) =>
        batch.write
          .options(postgreOptions)
          .option("dbtable", config.getString("newsTableName"))
          .mode(SaveMode.Overwrite)
          .saveAsTable(config.getString("tableName"))
      }
      .start()
    //      .awaitTermination()
  }

  def transformNews(ds: Dataset[News]): DataFrame = {
    ds
      //    .select(expr("cast(value as string) as actualValue"))
      .select(col("title"), col("publication_date"))
      .withColumn("quantity", lit(1))
      .withColumn("tmst", to_timestamp(substring(col("publication_date"), 1, 19), "yyyy-MM-dd'T'HH:mm:ss"))
  }

  // Пишем датафрейм статистики новостей в консоль
  def writeNewsStatConsole(df: DataFrame): Unit = {
    println(s"\n$prefix Статистика по новостям. Начало. Консоль.\n")
    df.writeStream
      .option("checkpointLocation", checkpointLocation)
      .format("console")
      //      .outputMode("append")
      .outputMode("complete")
      .start()
      .awaitTermination()
    println(s"$prefix Статистика по новостям. Конец. Консоль.")
  }

  // Пишем датафрейм статистики новостей в файл
  def writeNewsStatFile(df: DataFrame): Unit = {
    println(s"\n\n$prefix Статистика по новостям. Начало. Файл.\n\n")
    val sq =
      df.writeStream
        .format("csv")
        .option("checkpointLocation", checkpointLocation)
        .outputMode("append")
        .option("path", "output_stat/")
        .start()
    //      .awaitTermination()

    println(s"$prefix Статистика по новостям. Конец. Файл.")
    //        sq.awaitTermination()
  }

  def createWordsFromNews(ds: Dataset[News]): DataFrame = {
    val wordsExclude = Seq("в", "на", "под", "после", "без", "во", "для", "за", "и", "из", "из-за", "между", "кроме", "о", "от", "надо", "перед", "при", "я", "он", "мы",
      "они", "она", "через", "у", "к", "ради", "сквозь", "за", "вне", "до", "с", "по", "c", "а", "не", "чем", "об", "здесь",
      "еще", "над", "них", "его", "что", "как")

    ds
      .select(
        col("id").as("news_id"),
        col("title"),
        coalesce(unix_timestamp(substring(col("publication_date"), 1, 19), "yyyy-MM-dd'T'HH:mm:ss"), lit(0)).as("publication_uts"),
        unix_timestamp(current_timestamp()).as("dwh_uts")
      )
      .withColumn("word", explode(split(lower(column("title")), "\\s")))
      .filter(!col("word").isin(wordsExclude: _*))

  }

  // Пишем датафрейм слов в БД
  def writeWordsPostgres(spark: SparkSession, df: DataFrame): Unit = {
    println(s"\n\n$prefix Формирование слов\n\n")

    import spark.implicits._
    val dsWords = df
      .select(col("news_id"),
        col("word"),
        col(("publication_uts")),
        col("dwh_uts")
      )
      .as[Words]

    val sq = dsWords
      .writeStream
      .option("checkpointLocation", checkpointLocation)
      //      .trigger(Trigger.ProcessingTime(2.seconds))
      .foreachBatch { (batch: Dataset[Words], _: Long) =>
        batch.write
          .format("jdbc")
          .options(postgreOptions)
          .option("dbtable", config.getString("wordsTableName"))
          .mode(SaveMode.Append)
          //          .mode(SaveMode.Overwrite)
          //          .mode("complete")
          .save()
      }
      .start()
    //  .awaitTermination()

    sq.awaitTermination()
  }

  // Такая агрегация не работает в стриминге, чтобы при этом еще и писать результат в БД
  // См. пункт "Unsupported Operations" по этой ссылке:
  // https://spark.apache.org/docs/latest/structured-streaming-programming-guide.html#handling-late-data-and-watermarking
  //
  // There are a few DataFrame/Dataset operations that are not supported with streaming DataFrames/Datasets. Some of them are as follows.
  //
  //Multiple streaming aggregations (i.e. a chain of aggregations on a streaming DF) are not yet supported on streaming Datasets.
  //
  //Limit and take the first N rows are not supported on streaming Datasets.
  //
  //Distinct operations on streaming Datasets are not supported.
  //
  //Deduplication operation is not supported after aggregation on a streaming Datasets.
  //
  //Sorting operations are supported on streaming Datasets only after an aggregation and in Complete Output Mode.
  //
  //Few types of outer joins on streaming Datasets are not supported. See the support matrix in the Join Operations section for more details.
  //
  def createSlideAggWindow(df: DataFrame): DataFrame = {
    df
      //      .withWatermark("tmst","720 minutes")
      //      .groupBy(window(col("tmst"), "720 minutes").as("time"),col("word"))
      .withWatermark("tmst", "720 minutes")
      .groupBy(window(col("tmst"), "600 minutes", "1 minute").as("time"), col("word"))
      //      .withWatermark("tmst","600 minutes")
      //      .groupBy(window(col("tmst"), "600 minutes", "60 seconds").as("time"),col("word"))
      //      .withWatermark("tmst","1 minute")
      //      .groupBy(col("word"), window(col("tmst"), "1 minute" ).as("time"))
      //
      // .groupBy(col("word"),window(col("tmst"), "60 seconds", "20 seconds").as("time"))
      .agg(sum(col("cnt")).as("quantity"))
      //      .count().as("quantity")
      .select(
        col("word"),
        col("quantity")
        //        ,col("time").getField("start").as("wnd_start"),
        //        col("time").getField("end").as("wnd_end")
      )
    //      .filter(col("quantity")>1)
  }

  // Пишем датафрейм слов в БД
  def writeWordsCntPostgres(spark: SparkSession, df: DataFrame): Unit = {
    println(s"\n\n$prefix Количество слов. Начало. БД.\n\n")

    import spark.implicits._
    val dsWords = df
      .as[WordsCount]

    try {
      dsWords
        .writeStream
        .option("checkpointLocation", checkpointLocation)
        .foreachBatch { (batch: Dataset[WordsCount], _: Long) =>
          batch.write
            .format("jdbc")
            .options(postgreOptions)
            .option("dbtable", config.getString("wordsCountTableName"))
            .mode(SaveMode.Append)
            //            .mode("complete")
            //            .mode(SaveMode.Overwrite)
            .save()
        }
        .start()
        .awaitTermination()
    }
    catch {
      case e: BatchUpdateException => e.getNextException()
      case e: Exception => println(s"\n\n$prefix: ${e.getMessage}\n\n${e.getStackTrace.mkString("\n")}\n\n ${}")
    }

    //    println(s"$prefix Количество слов. Конец. БД.")
  }

  def getNews(spark: SparkSession): Unit = {

    val news = readStreamNews(spark)
    writeNewsPostgres(news)
    //    writeNewsFile(news)
    //    writeNewsConsole(news)

    val words = createWordsFromNews(news)
    writeWordsPostgres(spark, words)
    //      writeNewsStatConsole(words)


    //    val wordsCnt = createSlideAggWindow(words)
    //    writeWordsCntPostgres(spark, wordsCnt)
    //    writeNewsStatConsole(wordsCnt)
  }
}
