package dev.cmartin.scrapper

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*

object ScraperApplication {
  val plateRegex  = """^\d{2}\*{2}\s[A-Z]{3}$""".r
  val WEBSITE_URL = "https://www.dieselogasolina.com/ultima-matricula.html"

  def main(args: Array[String]): Unit = {

    val browser = new JsoupBrowser()
    val doc     = browser.get(WEBSITE_URL)

    val elements = doc >> elementList("span")

    val plateOption = elements.map(e => e >> text("span"))
      .find(a => plateRegex.matches(a))

    val result = plateOption.fold("Plate not found")(identity)

    println(s"last plate: ${result}")
  }

}
