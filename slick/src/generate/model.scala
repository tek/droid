package slick.db

import org.joda.time.DateTime

trait Model
{
  def id: Option[Long]
}

object Model
{
  implicit class `DB Model sequence`[A <: Model](seq: Seq[A]) {
    def ids = seq.map(_.id).flatten.toSet
  }
}

trait Timestamps
{
  var dateCreated: DateTime
  var lastUpdated: DateTime
}
