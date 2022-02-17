package HotNews

import com.typesafe.config.{ ConfigFactory}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}
import org.apache.spark.sql.functions._

object Publisher {
  val config = ConfigFactory.load()
  val checkpointLocation = config.getString("checkpointLocation")
  val wordsLimit = config.getString("wordsLimit").toInt
  val newsLimit = config.getString("newsLimit").toInt
  val windowDuration = config.getString("windowDuration").toInt
  val sleepDuration = config.getString("sleepDuration").toInt
  val startDate = config.getString("startDate").toLong
  val endDate = config.getString("endDate").toLong
  val period = config.getString("period").toInt

  val postgreOptions = Map(
    "user" -> config.getString("userDB"),
    "password" -> config.getString("password"),
    "driver" -> config.getString("driver"),
    "url" -> config.getString("url")
  )

  val prefix = "How news!!! >>>  "

  def getHotNews(spark: SparkSession): Unit = {
    var j = startDate - period
    for (i <- startDate.to(endDate, -period)) {
      getWindowedNewsPublishDate(spark, j, i)
      j = j - period
      //      getWindowedNewsCurrentDate(spark,windowDuration)
      Thread.sleep(sleepDuration * 1000)
    }
  }

  //Горячие новости по дате публикации
  def getWindowedNewsPublishDate(spark: SparkSession, startTs: Long, endTs: Long): Unit = {

    import spark.implicits._

    val words = spark
      .read
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("wordsTableName"))
      .load()

    val news = spark
      .read
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("newsTableName"))
      .load()

    val dfTopWords = words
      .filter(col("publication_uts") >= lit(startTs) && col("publication_uts") < lit(endTs))
      .groupBy(col("word")
      )
      .agg(sum(lit(1)).as("quantity"))
      .orderBy(col("quantity").desc) //, col("publiction_date"))
      .limit(wordsLimit)
      .select(
        col("word"),
        col("quantity"),
        lit(startTs).as("wnd_start"),
        lit(endTs).as("wnd_end")
      )

    //    println(s"\n###########################\n Самые используемые слова \n###########################\n")
    //    dfTopWords.show()

    val dfTopNews = dfTopWords.as("tw")
      .join(words.as("w"), col("tw.word") === col("w.word") &&
        col("w.publication_uts") >= col("wnd_start") &&
        col("w.publication_uts") < col("wnd_end"))
      .withColumn("rn", row_number().over(Window.partitionBy("news_id").orderBy("publication_uts")))
      .filter(col("rn") > lit(1))
      //      .orderBy(col("rn").desc,col("quantity").desc)
      //      .dropDuplicates("news_id")
      .join(news.as("n"), col("id") === col("news_id"))
      .orderBy(col("rn").desc, col("quantity").desc)
      .dropDuplicates("news_id")
      .limit(newsLimit)
      .select(col("title"))

    val processDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(endTs * 1000))
    println(s"\n#############################\nГорячие новости за $processDate \n#############################\n")
    dfTopNews.foreach(e => println(e))
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

  //Горячие новости по дате их поступления из кафки
  def getWindowedNewsCurrentDate(spark: SparkSession, windowDuration: Int): Unit = {
    import spark.implicits._

    val words = spark
      .read
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("wordsTableName"))
      .load()

    val news = spark
      .read
      .format("jdbc")
      .options(postgreOptions)
      .option("dbtable", config.getString("newsTableName"))
      .load()

    val dfTopWords = words
      .filter(col("dwh_uts") >= unix_timestamp(current_timestamp()) - windowDuration && col("dwh_uts") < unix_timestamp(current_timestamp()))
      .groupBy(col("word")
      )
      .agg(sum(lit(1)).as("quantity"))
      .orderBy(col("quantity").desc)
      .limit(wordsLimit)
      .select(
        col("word"),
        col("quantity"),
        (unix_timestamp(current_timestamp()) - windowDuration).as("wnd_start"),
        unix_timestamp(current_timestamp()).as("wnd_end")
      )

    println(s"\n###########################\n Самые используемые слова \n###########################\n")
    dfTopWords.show()

    val dfTopNews = dfTopWords.as("tw")
      .join(words.as("w"), col("tw.word") === col("w.word") &&
        col("w.dwh_uts") >= col("wnd_start") &&
        col("w.dwh_uts") < col("wnd_end"))
      .withColumn("rn", row_number().over(Window.partitionBy("news_id").orderBy("dwh_uts")))
      .filter(col("rn") > lit(1))
      .orderBy(col("rn").desc, col("quantity").desc)
      .dropDuplicates("news_id")
      .join(news.as("n"), col("id") === col("news_id"))
      .orderBy(col("rn").desc, col("quantity").desc)
      .limit(newsLimit)

    println(s"\n###########################\n      Горячие новости за  \n###########################\n")
    //    dfTopNews.show()
    dfTopNews.foreach(e => println(e))
  }

}
