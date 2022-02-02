package HotNews

case class News(source: String,
                title: String,
                text: String,
                publication_date: String,
                rubric: String,
                subrubric: String,
                tags: String)

object News {
  def apply(a: Array[String]): News =
    News(
      a(0),
      a(1),
      a(2),
      a(3),
      a(4),
      a(5),
      a(6)
    )
}