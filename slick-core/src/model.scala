package slick.db

import com.github.nscala_time.time.Imports.DateTime

trait Model
{
  def id: Long
}

object Model
{
  implicit class `DB Model sequence`[A <: Model](seq: Seq[A]) {
    def ids = seq.map(_.id).toSet
  }
}

trait Timestamps[A]
{
  val created: DateTime
  val updated: DateTime
  def withDate(u: DateTime): A
}

trait Uuids
{
  def uuid: Option[String]
}

object Uuids
{
  implicit class `seq of Uuids`(seq: Seq[_ <: Uuids]) {
    def uuids = seq map(_.uuid)

    def flatUuids = uuids flatten
  }
}

trait Sync
extends Uuids
