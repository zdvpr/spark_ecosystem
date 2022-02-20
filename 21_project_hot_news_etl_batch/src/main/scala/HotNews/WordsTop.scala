package HotNews

case class WordsTop(
                  word: String
                )

object WordsTop {
  def apply(a: Array[String]): WordsTop =
    WordsTop(
      a(0)
    )
}