package slick.test

import org.specs2._
import org.specs2.specification._

@TestSchema()
object InfoTest
{
  case class Alpha(name: String)
  case class Beta(name: String, alp: Alpha)
  case class Gamma(bets: List[Beta], opt: Option[Alpha], alp: Alpha,
    style: Int)
}

class InfoTest
{
  foo = 1
}
