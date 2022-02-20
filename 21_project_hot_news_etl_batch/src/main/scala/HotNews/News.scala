package HotNews

case class News(
                offset: Long,
                id: Int,
                source: String,
                title: String,
                full_text: String,
                publication_date: String
  )

object News {
  def apply(offset:Long, a: Array[String]): News =
    News(
      offset,
      a(0).toInt,
      a(1),
      a(2),
      a(3),
      a(4)
    )
}