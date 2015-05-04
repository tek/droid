package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import scalaz._, Scalaz._

import org.joda.time.DateTime

import slick._

@Schema()
object PendingActionsSchema
{
  case class Addition(target: Long)
  case class Update(target: Long)
  case class Deletion(target: String)
  case class PendingActionSet(model: String, additions: List[Addition],
    updates: List[Update], deletions: List[Deletion])
}

trait PendingActionsCrudEx[
A <: Model with Uuids with Timestamps,
B <: Table[A] with TableEx[A]
]
extends CrudEx[A, B]
{ self: TableQuery[B] ⇒

  import PendingActionsSchema._

  def name: String

  def pendingActions = PendingActionsSchema.pendingActionSets

  def pending(implicit s: JdbcBackend#SessionDef) = pendingActions.filter {
    _.model === name }.firstOption.orElse {
      pendingActions.insert(PendingActionSet(None, name))
    }

  override def insert(obj: A)(implicit s: JdbcBackend#SessionDef) = {
    val added = super.insert(obj)
    for {
      sets ← pending
      o ← added
      oid ← o.id
      a ← additions.insert(Addition(None, oid))
      id ← a.id
    } sets.addAddition(id)
    added
  }

  override def update(obj: A)(implicit s: JdbcBackend#SessionDef) = {
    for {
      sets ← pending
      oid ← obj.id
      a ← updates.insert(Update(None, oid))
      id ← a.id
    } sets.addUpdate(id)
    super.update(obj)
  }

  def delete(obj: A)(implicit s: JdbcBackend#SessionDef) = {
    for {
      sets ← pending
      uuid ← obj.uuid
      a ← deletions.insert(Deletion(None, uuid))
      id ← a.id
    } sets.addDeletion(id)
    obj.id foreach(deleteId)
  }

  def completeSync(obj: A)(implicit s: JdbcBackend#SessionDef) = {
    val adds = for {
      p ← pendingActions if p.model === name
      a ← p.additions
      if a.target === obj.id
    } yield a.id
    pending foreach { _.deleteAdditions(adds.list) }
    val ups = for {
      p ← pendingActions if p.model === name
      a ← p.updates
      if a.target === obj.id
    } yield a.id
    pending foreach { _.deleteUpdates(ups.list) }
  }

  def completeDeletion(uuid: String)(implicit s: JdbcBackend#SessionDef) = {
    val dels = for {
      p ← pendingActions if p.model === name
      a ← p.deletions
      if a.target === uuid
    } yield a.id
    pending foreach { _.deleteDeletions(dels.list) }
  }

  override def deleteById(id: Long)(implicit s: JdbcBackend#SessionDef) =
    byId(id) foreach(delete)
}

abstract class SyncTableQuery[
A <: Types#ExtModel,
B <: Types#ExtTable[A]]
(cons: (Tag) ⇒ B)
extends TableQuery[B](cons)
with PendingActionsCrudEx[A, B]
