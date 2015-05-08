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

trait Timestamps[A]
{
  val created: Option[DateTime]
  val updated: Option[DateTime]
  def withDates(c: Option[DateTime] = None, u: Option[DateTime] = None): A
}

trait Uuids
{
  def uuid: Option[String]
}

object Uuids
{
  implicit class `seq of Uuids`(seq: Seq[_ <: Uuids]) {
    def uuids = seq map(_.uuid)
  }
}

trait Sync
extends Uuids
