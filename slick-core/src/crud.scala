package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import scalaz._, Scalaz._

import org.joda.time.DateTime

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
      row ← self if row.id === obj.id.getOrElse(0L)
    } yield row
    q update obj
  }

  override def insert(obj: C)(implicit s: Session) = {
    // returning is not supported in SQLDroid
    // val res = self returning (self.map(_.id)) insert (obj)
    super.insert(obj)
    val q = for { o ← self.sortBy(_.id.desc).take(1) } yield o
    q.list.headOption
  }

  def delete(obj: C)(implicit s: Session) = obj.id foreach(deleteId)
}

// FIXME replace null with None
trait CrudEx[C <: Model with Timestamps,
T <: Table[C] with TableEx[C]] extends Crud[C, T] {
  self: TableQuery[T] ⇒
  override def update(obj: C)(implicit s: Session) = {
    if (obj.dateCreated == null)
      obj.dateCreated = obj.id flatMap(byId) map(_.dateCreated) getOrElse {
        DateTime.now
      }
    obj.lastUpdated = DateTime.now
    super.update(obj)
  }

  override def insert(obj: C)(implicit s: Session) = {
    // because x.copy(dateCreated = , lastUpdated = ) is not available for
    // type parameters :(
    obj.dateCreated = DateTime.now
    obj.lastUpdated = obj.dateCreated
    super.insert(obj)
  }
}
