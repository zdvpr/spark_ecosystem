package les2


import java.io.{FileOutputStream, PrintStream}

import io.circe.{Decoder, HCursor, parser}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.Decoder.Result

import scala.io.Source

object TopCountries extends App {

  case class Country(
                      name: String,
                      capital: List[String],
                      region: String,
                      area: Double)

  case class CountryOut(
                         name: String,
                         capital: String,
                         area: Double)

  case class ProductResrsource(name: String, campaignResources: List[Int], discountPrice: List[Int])

  implicit val decoder: Decoder[Country] = new Decoder[Country] {
    override def apply(hCursor: HCursor): Result[Country] =
      for {
        name <- hCursor.downField("name").downField("official").as[String]
        capital <- hCursor.downField("capital").as[List[String]]
        region <- hCursor.downField("region").as[String]
        area <- hCursor.downField("area").as[Double]
      } yield
        Country(name, capital, region, area)
  }

  def source = Source.fromURL(
    "https://raw.githubusercontent.com/mledoze/countries/master/countries.json"
  ).getLines.mkString.stripMargin



  parser.decode[List[Country]](source) match {
    case Right(countries) => {
      val countriesSel = countries.filter(_.region == "Africa").sortBy(_.area)(Ordering[Double].reverse).take(10)
      var countriesOut =  List[CountryOut] ()
      for (e <- countriesSel)
        countriesOut = countriesOut :+ CountryOut(e.name,e.capital(0),e.area)
      val fos = new FileOutputStream(args(0))
      val printer = new PrintStream(fos)
      printer.println(countriesOut.asJson)
    }
    case Left(ex) => println(s"Something wrong with json file decoding: ${ex}")
  }

}


