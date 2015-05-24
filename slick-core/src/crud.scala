package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import scalaz._, Scalaz._

import com.github.nscala_time.time.Imports.DateTime

trait TableEx[C <: Model] {
  def id: Column[Long]
}

trait CrudCompat[C, T <: Table[C]]
{
  self: TableQuery[T] ⇒

  def insert(obj: C)(implicit s: Session) = {
    self += obj
    some(obj)
  }
}

trait Crud[C <: Model, T <: Table[C] with TableEx[C]]
extends CrudCompat[C, T]
{
  self: TableQuery[T] ⇒

  def deleteById(id: Long)(implicit s: Session) {
    deleteId(id)
  }

  def deleteId(objId: Long)(implicit s: Session) =
    self.filter { _.id === objId }.delete

  def byId(objId: Long)(implicit s: Session) =
    self.filter(_.id === objId).firstOption

  def byIds(ids: Traversable[Long])(implicit s: Session) =
    self.filter(_.id inSet(ids)).list

  def update(obj: C)(implicit s: Session) = {
    val q = for {
      row ← self if row.id === obj.id
    } yield row
    q update obj
    Some(obj)
  }

  override def insert(obj: C)(implicit s: Session) = {
    // returning is not supported in SQLDroid
    // val res = self returning (self.map(_.id)) insert (obj)
    super.insert(obj)
    val q = for { o ← self.sortBy(_.id.desc).take(1) } yield o
    q.list.headOption
  }

  def delete(obj: C)(implicit s: Session) = deleteId(obj.id)
}

trait CrudEx[C <: Model with Timestamps[C],
T <: Table[C] with TableEx[C]] extends Crud[C, T] {
  self: TableQuery[T] ⇒
  override def update(obj: C)(implicit s: Session) = {
    super.update(obj.withDate(DateTime.now))
  }
}
