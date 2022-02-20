package HotNews

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}
import org.apache.spark.sql.functions._

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

  val prefix = "Batch hot news!!! >>>  "

  def readNews(spark: SparkSession, startOffset: Long, busDay: Long//, endOffset: Int
                ): Dataset[News] = {
    val bootstrapServers = config.getString("kafka.bootstrap.servers")
    val topic = config.getString("kafka.topic")

    import spark.implicits._

    println(s"\n\n$prefix Считыванеи данных из кафки\n\n")

    spark
      .read
      .format("kafka")
      .option("kafka.bootstrap.servers", bootstrapServers)
      .option("subscribe", topic)
      .option("startingOffsets", s"""{"$topic":{"0":$startOffset}}""")
//      .option("endingOffsets", s"""{"$topic":{"0":$endOffset}}""")
      .load()
      .selectExpr("CAST(offset AS Long)","CAST(value AS STRING)")
      .as[(Long,String)]
      .map(e => News(e._1, e._2.replaceAll("[,.\"«»!?—]", "").replace("\u00A0", " ").split(";&#")))
  }

  // Пишем считаеми и записываем в Postgres
  def setFirstOffsetNextDay(dsNews: Dataset[News],busDay: Long, nextBusDay: Long): Unit = {
    println(s"\n\n$prefix Расчет первого офсета\n\n")

    dsNews
      .filter(col("publication_uts")< lit(busDay))
      .filter(col("publication_uts")>= lit(nextBusDay))
      .agg(min(col("offset"))).as("first_offset")
      .select(
        lit(nextBusDay).as("bus_date_uts"),
        col("first_offset")
      )
      .write
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("offsetTableName"))
      .mode(SaveMode.Append)
      .save()
  }

  def getFirstOffsetBusDay(spark: SparkSession,busDay: Long): Long = {
    println(s"\n\n$prefix Считывание первого офсета для заданного дня\n\n")

    spark
      .read
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("offsetTableName"))
      .load()
      .filter(col("bus_date_uts")===lit(busDay))
      .first().getLong(1)
  }

  // Пишем датасет новостей в Postgres
  def writeNewsPostgres(dsNews: Dataset[News],busDay: Long): DataFrame = {
    println(s"\n\n$prefix Запись новостей в БД\n\n")

    val dsDayNews = dsNews
//      .coalesce(unix_timestamp(substring(col("publication_date"), 1, 19), "yyyy-MM-dd'T'HH:mm:ss"),lit(0)).as("publication_uts")
//      .filter(col("publication_uts")>=lit(busDay))
//      .filter(col("publication_uts")<lit(prevBusDay))


    dsDayNews
      .drop(col("offset"))
      .write
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("newsTableName"))
      .mode(SaveMode.Append)
      .save()

    dsDayNews
      .drop(col("publication_date"))

  }

  // Пишем датасет слов в Postgres
  def writeWordsPostgres(spark: SparkSession, df: DataFrame): Unit = {
    println(s"\n\n$prefix Запись слов в БД\n\n")

  import spark.implicits._

  val dsWords = df
    .select(col("news_id"),
      col("word"),
      col(("publication_uts")),
      col("dwh_uts")
    )
    .as[Words]

    dsWords
      .write
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("wordsTableName"))
      .mode(SaveMode.Append)
      .save()
  }

  def createWordsFromNews(ds: DataFrame): DataFrame = {

    val wordsExclude = Seq("в", "на", "под", "после", "без", "во", "для", "за", "и", "из", "из-за", "между", "кроме", "о", "от", "надо", "перед", "при", "я", "он", "мы",
      "они", "она", "через", "у", "к", "ради", "сквозь", "за", "вне", "до", "с", "по", "c", "а", "не", "чем", "об", "здесь",
      "еще", "над", "них", "его", "что", "как")

    ds
      .select(
        col("id").as("news_id"),
        col("title"),
//        coalesce(unix_timestamp(substring(col("publication_date"), 1, 19), "yyyy-MM-dd'T'HH:mm:ss"), lit(0)).as("publication_uts"),
        unix_timestamp(current_timestamp()).as("dwh_uts")
      )
      .withColumn("word", explode(split(lower(column("title")), "\\s")))
      .filter(!col("word").isin(wordsExclude: _*))

  }


  def getNews(spark: SparkSession, busDay: Long, nextBusDay: Long): Unit = {

    val startOffset =  getFirstOffsetBusDay(spark, busDay)

    val news = readNews(spark, startOffset,busDay)
    val newsDay = writeNewsPostgres(news, busDay)

    setFirstOffsetNextDay(news,busDay, nextBusDay)

    val words = createWordsFromNews(newsDay)
    writeWordsPostgres(spark, words)
  }
}
