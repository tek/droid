package slick.test

import slick._
import slick.db._

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
with schema.Uuids
with schema.Sync
{
  case class Alpha(name: String)
  case class Beta(name: String, alp: Alpha, alp2: Alpha)
  case class Gamma(name: String, bet: List[Beta], bet2: List[Beta])
}
