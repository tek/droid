package slick.test

import slick.{Schema, SyncSchema, schema}

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
  case class Gamma(name: String, bet: List[Beta])
}

@SyncSchema()
object ExtTestSchema
extends schema.Timestamps
{
  object Flag extends Enumeration {
    type Flag = Value
    val On = Value
    val Off = Value
  }

  case class Alpha(name: String, flog: Flag, num: Double)
  case class Beta(name: String, killed: Option[DateTime], alp: Alpha,
    alp2: Alpha)
  case class Gamma(name: String, bet: List[Beta], bet2: List[Beta])
}
