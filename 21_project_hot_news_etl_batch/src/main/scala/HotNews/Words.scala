package HotNews

case class Words(
                  news_id: Int,
                  word: String,
                  publication_uts: Long,
                  dwh_uts: Long
                )

object Words {
  def apply(a: Array[String]): Words =
    Words(
      a(0).toInt,
      a(1),
      a(2).toLong,
      a(3).toLong
    )
}