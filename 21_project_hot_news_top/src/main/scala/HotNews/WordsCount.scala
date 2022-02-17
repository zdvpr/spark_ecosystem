package HotNews

case class WordsCount(
                  word: String,
                  quantity: Long
//                  ,wnd_start: String,
//                  wnd_end: String
                )

object WordsCount {
  def apply(a:String, b: Long, c: String, d: String): WordsCount =
    WordsCount(
      a,
      b
//      ,c,
//      d
    )
}
