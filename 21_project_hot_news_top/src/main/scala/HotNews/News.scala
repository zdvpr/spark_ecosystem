package HotNews

case class News(
                id: Int,
                source: String,
                title: String,
                full_text: String,
                publication_date: String,
                rubric: String,
                subrubric: String
  )

object News {
  def apply(a: Array[String]): News =
    News(
      a(0).toInt,
      a(1),
      a(2),
      a(3),
      a(4),
      a(5),
      a(6)
    )
}