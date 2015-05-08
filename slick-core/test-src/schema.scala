package slick.test

import org.specs2._
import org.specs2.specification._

@TestSchema()
object BasicTest
{
  object Flag extends Enumeration {
    type Flag = Value
    val On = Value
    val Off = Value
  }
  import Flag._

  case class Alpha(name: String, flog: Flag)
  case class Beta(name: String, alp: Alpha)
  case class Gamma(bets: List[Beta], opt: Option[Alpha], alp: Alpha,
    style: Int)
}

class BasicTest
{
  foo = 1
}
