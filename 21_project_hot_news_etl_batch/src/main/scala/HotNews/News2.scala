package HotNews

case class News2(
                 offset: Long,
                 value: String
               )

object News2 {
  def apply(offset:Long, value: String): News2 =
    News2(
      offset,
      value
    )
}