package dev.cmartin.scrapper

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScraperTest
    extends AnyFlatSpec
    with Matchers {

  behavior of "Scraper"

  val plateRegex = """^\d{2}\*{2}\s[A-Z]{3}$""".r

  val WEBSITE_URL        = "https://www.dieselogasolina.com/ultima-matricula.html"
  val ENERGY_WEBSITE_URL = "https://tarifaluzhora.es/"

  it should "retrieve last seen plate" in {
    val browser = new JsoupBrowser()
    val doc     = browser.get(WEBSITE_URL)

    // info(doc.toHtml)

    val elements = doc >> elementList("span")

    val result = elements.map(e => e >> text("span"))
      .find(a => plateRegex.matches(a))

    info(s"$result")
  }

  it should "retrieve best hour range" in {
    val browser = new JsoupBrowser()
    val doc     = browser.get("https://tarifaluzhora.es/")

    /* doc for local test */
    // val doc     = browser.parseFile("scraper/src/test/resources/tarifaluzhora.html")

    // info(doc.toHtml)

    // Extract the elements with class ".active"
    val divs     = doc >> "div"
    val elements = divs >> elementList(".template-tlh__big-text")
    val result   = elements.headOption >> text("p")

    info(s"divs: ${divs.size}, elements: ${elements.size}")

    info(s"$result")
  }

  it should "match the plate regex" in {
    val result = plateRegex.matches("50** MSK")

    result shouldBe true
  }

}
