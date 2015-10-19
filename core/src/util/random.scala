package tryp.util

import scala.util.Random

object Generator {
  def string(length: Int = 10) = Random.alphanumeric.take(length).mkString
}
