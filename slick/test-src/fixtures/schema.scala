package slick.test

import slick.{Schema, SyncSchema, Timestamps}

@Schema()
object SimpleTestSchema
{
  object Flag extends Enumeration {
    type Flag = Value
    val On = Value
    val Off = Value
  }

  case class Alpha(name: String, flagger: Flag)
  case class Beta(name: String, alp: Alpha)
  case class Gamma(name: String, bets: List[Beta])
}

@SyncSchema()
object ExtTestSchema
extends Timestamps
{
  object Flag extends Enumeration {
    type Flag = Value
    val On = Value
    val Off = Value
  }

  case class Alpha(name: String, flog: Flag, num: Double)
  case class Beta(name: String, killed: Option[DateTime], alp: Alpha,
    alp2: Alpha)
  case class Gamma(name: String, bets: List[Beta], bet2s: List[Beta])
}
