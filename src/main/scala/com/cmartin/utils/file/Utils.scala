package com.cmartin.utils.file

import scala.Console.BLUE
import scala.Console.GREEN
import scala.Console.RED
import scala.Console.RESET
import scala.Console.YELLOW

//TODO refactor to package object
object Utils {
  def prettyPrint[T](list: List[T]): String =
    list.mkString(s"list size (${list.size}) =>\n[\n", ",\n", "\n]")

  def colourRed(text: String): String =
    colour(text, RED)

  def colourYellow(text: String): String =
    colour(text, YELLOW)

  def colourGreen(text: String): String =
    colour(text, GREEN)

  def colourBlue(text: String): String =
    colour(text, BLUE)

  private def colour(text: String, color: String): String =
    s"$RESET$color$text$RESET"
}
