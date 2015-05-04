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

  def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
    self += obj
    some(obj)
  }
}

trait Crud[C <: Model, T <: Table[C] with TableEx[C]]
extends CrudCompat[C, T]
{
  self: TableQuery[T] ⇒

  def deleteById(id: Long)(implicit session: JdbcBackend#SessionDef) {
    deleteId(id)
  }

  def deleteId(objId: Long)(implicit session: JdbcBackend#SessionDef) =
    self.filter { _.id === objId }.delete

  def byId(objId: Long)(implicit session: JdbcBackend#SessionDef) =
    self.filter(_.id === objId).firstOption

  def byIds(ids: Traversable[Long])(implicit session: JdbcBackend#SessionDef) =
    self.filter(_.id inSet(ids)).list

  def update(obj: C)(implicit session: JdbcBackend#SessionDef) = {
    (for {row ← self if row.id === obj.id.get} yield row) update (obj)
  }

  override def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
    // returning is not supported in SQLDroid
    // val res = self returning (self.map(_.id)) insert (obj)
    super.insert(obj)
    val q = for { o ← self.sortBy(_.id.desc).take(1) } yield o
    q.list.headOption
  }
}

trait CrudEx[C <: Model with Timestamps,
T <: Table[C] with TableEx[C]] extends Crud[C, T] {
  self: TableQuery[T] ⇒
  override def update(obj: C)(implicit session: JdbcBackend#SessionDef) = {
    obj.lastUpdated = DateTime.now
    super.update(obj)
  }

  override def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
    // because x.copy(dateCreated = , lastUpdated = ) is not available for
    // type parameters :(
    obj.dateCreated = DateTime.now
    obj.lastUpdated = obj.dateCreated
    super.insert(obj)
  }
}
