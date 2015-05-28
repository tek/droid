package slick.db

import com.github.nscala_time.time.Imports.DateTime

trait HasObjectId
{
  def id: ObjectId
}

object HasObjectId
{
  implicit class `seq of HasObjectId`(seq: Seq[_ <: HasObjectId]) {
    def ids = seq map(_.id)
  }
}

trait Model
extends HasObjectId

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
