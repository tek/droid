package slick.db

import scala.slick.driver.SQLiteDriver.simple._

import com.github.nscala_time.time.Imports.DateTime

object DbTypes
{
  type IdType = ObjectId
}

trait HasObjectId
{
  def id: DbTypes.IdType
}

object HasObjectId
{
  implicit class `seq of HasObjectId`(seq: Traversable[_ <: HasObjectId]) {
    def idList = seq map(_.id) toList
    def ids = idList.toSet
  }
}

trait Model
extends HasObjectId

trait Timestamps[A]
{
  val created: DateTime
  val updated: DateTime
  def withDate(u: DateTime): A
}
