package slick.dao

import scala.language.existentials
import scala.language.reflectiveCalls
import language.experimental.macros
import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend
import scala.slick.profile.RelationalTableComponent
import org.joda.time.DateTime

import slick._

object Crud {
  type TableEx[C] = {
    def id: Column[Long]
  }

  trait Crud[C <: db.Model, T <: Table[C] with TableEx[C]] {
    self: TableQuery[T] ⇒

    def deleteById(id: Long)(implicit session: JdbcBackend#SessionDef) {
      deleteId(id)
    }

    def deleteId(objId: Long)(implicit session: JdbcBackend#SessionDef) =
      self.filter { _.id === objId }.delete

    def byId(objId: Long)(implicit session: JdbcBackend#SessionDef) =
      self.filter(_.id === objId).firstOption

    def update(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      (for {row ← self if row.id === obj.id.get} yield row) update (obj)
    }

    def insert(obj: C)(implicit session: JdbcBackend#SessionDef) = {
      // val res = self returning (self.map(_.id)) insert (obj)
      self += obj
      val q = for { o ← self.sortBy(_.id.desc).take(1) } yield o
      q.list.headOption
    }
  }

  trait CrudEx[C <: db.Model with db.Timestamps,
  T <: Table[C] with TableEx[C]] extends Crud[C, T] {
    self: TableQuery[T] ⇒
    override def update(obj: C)(implicit session: JdbcBackend#SessionDef): Int = {
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
}
