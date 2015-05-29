package slick

import scala.slick.jdbc.JdbcBackend

trait ObjectIdAdapter
{
  type ObjectId = org.bson.types.ObjectId

  object ObjectId
  {
    def apply() = new ObjectId
    def apply(s: String) = new ObjectId(s)
  }
}

trait Globals
extends tryp.core.meta.Globals
