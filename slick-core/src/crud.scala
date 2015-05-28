package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import scalaz._, Scalaz._

import com.github.nscala_time.time.Imports.DateTime

object ObjectIdMapper
{
    implicit val objectIdMapper =
      MappedColumnType.base[ObjectId, String](
      { a ⇒ a.toString },
      { a ⇒ new ObjectId(a) }
    )
}
import ObjectIdMapper._

trait TableEx[A <: Model] {
  def id: Column[ObjectId]
}

trait CrudCompat[A, B <: Table[A]]
{
  self: TableQuery[B] ⇒

  def insert(obj: A)(implicit s: Session) = {
    self += obj
    some(obj)
  }
}

trait Crud[A <: Model, B <: Table[A] with TableEx[A]]
extends CrudCompat[A, B]
{
  self: TableQuery[B] ⇒

  def deleteById(id: ObjectId)(implicit s: Session) = {
    deleteId(id)
  }

  def deleteId(id: ObjectId)(implicit s: Session) =
    self.filter { _.id === id }.delete

  def byId(id: ObjectId)(implicit s: Session) =
    self.filter(_.id === id).firstOption

  def byIds(ids: Traversable[ObjectId])(implicit s: Session) =
    self.filter(_.id inSet(ids)).list

  def byIdEither(id: ObjectId)(implicit s: Session) =
    byId(id) map(_.right) getOrElse("Not found".left)

  def update(obj: A)(implicit s: Session) = {
    val q = for {
      row ← self if row.id === obj.id
    } yield row
    q update obj
    Some(obj)
  }

  override def insert(obj: A)(implicit s: Session) = {
    // returning is not supported in SQLDroid
    // val res = self returning (self.map(_.id)) insert (obj)
    super.insert(obj)
    val q = for { o ← self.sortBy(_.id.desc).take(1) } yield o
    q.list.headOption
  }

  def delete(obj: A)(implicit s: Session) = deleteId(obj.id)

  def idExists(id: ObjectId)(implicit s: Session) =
    self.filter(_.id === id).exists.run
}

trait CrudEx[A <: Model with Timestamps[A],
B <: Table[A] with TableEx[A]] extends Crud[A, B] {
  self: TableQuery[B] ⇒
  override def update(obj: A)(implicit s: Session) = {
    super.update(obj.withDate(DateTime.now))
  }
}
