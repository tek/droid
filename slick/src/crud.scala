package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import argonaut._, Argonaut._

import scalaz._, Scalaz._

import com.github.nscala_time.time.Imports.DateTime

import slick._

trait Action[+A]
{
  def id: Long
  def target: A
}

@Schema()
object PendingActionsSchema
{
  case class Addition(target: Long)
  extends Action[Long]

  case class Update(target: Long)
  extends Action[Long]

  case class Deletion(target: String)
  extends Action[String]

  case class PendingActionSet(model: String, additions: List[Addition],
    updates: List[Update], deletions: List[Deletion])

  implicit class ActionSeq[A](seq: Iterable[Action[A]]) {
    def targets = seq map { _.target }
  }
}

import PendingActionsSchema._

trait SyncTable[A <: Model with Sync]
extends TableEx[A]
{
  def uuid: Column[Option[String]]
}

// FIXME Timestamps is not needed
// but exists in CrudEx
trait SyncCrud[
A <: Model with Sync with Timestamps[A],
B <: Table[A] with SyncTable[A]
]
extends CrudEx[A, B]
{ self: TableQuery[B] ⇒

  def path: String

  def pending(implicit s: Session): Option[PendingActionSet] =
    PendingActionSet.filter { _.model === path }.firstOption.orElse {
      PendingActionSet.insert(PendingActionSet(path))
    }

  override def insert(obj: A)(implicit s: Session) = {
    val added = super.insert(obj)
    for {
      sets ← pending
      o ← added
      a ← Addition.insert(Addition(o.id))
    } sets.addAddition(a.id)
    added
  }

  override def update(obj: A)(implicit s: Session) = {
    for {
      sets ← pending
      a ← Update.insert(Update(obj.id))
    } sets.addUpdate(a.id)
    super.update(obj)
  }

  def insertUnrecorded(obj: A)(implicit s: Session) = {
    super.insert(obj)
  }

  def updateUnrecorded(obj: A)(implicit s: Session) = {
    super.update(obj)
  }

  override def delete(obj: A)(implicit s: Session) = {
    for {
      sets ← pending
      uuid ← obj.uuid
      a ← Deletion.insert(Deletion(uuid))
    } sets.addDeletion(a.id)
    super.delete(obj)
  }

  override def deleteById(id: Long)(implicit s: Session) =
    byId(id) foreach(delete)

  def uuidById(id: Long)(implicit s: Session) =
    byId(id) flatMap { _.uuid }

  def idByUuid(uuid: String)(implicit s: Session) =
    self.filter(_.uuid === uuid).firstOption map { _.id }

  def idsByUuids(uuids: Iterable[String])(implicit s: Session) = {
    val q = for {
      o ← self if o.uuid inSet(uuids)
    } yield o.id
    q.list
  }
}

trait SyncTableQueryBase
{
  def path: String
  def pending(implicit s: Session): Option[PendingActionSet]
  def jsonForIds(ids: Iterable[Long])(implicit s: Session): Iterable[String]
  def setUuidFromJson(id: Long, json: String)(implicit s: Session)
  def syncFromJson(json: String)(implicit s: Session)
  def completeSync(a: Action[Long])(implicit s: Session)
  def completeDeletion(a: Deletion)(implicit s: Session)
  def uuidById(id: Long)(implicit s: Session): Option[Types#Uuid]
}

trait BackendMapper[A <: Types#ExtModel[A]]
extends Uuids

abstract class SyncTableQuery[
A <: Types#ExtModel[A],
B <: Types#ExtTable[A],
C <: BackendMapper[A]]
(cons: (Tag) ⇒ B)
extends TableQuery[B](cons)
with SyncTableQueryBase
with SyncCrud[A, B]
{
  import PendingActionsSchema._

  def name: String

  implicit def encodeJson(implicit s: Session): EncodeJson[A]
  implicit def mapperCodecJson(implicit s: Session): DecodeJson[C]

  def jsonForIds(ids: Iterable[Long])(implicit s: Session) =
    byIds(ids) map { _.asJson.spaces2 }

  def setUuidFromJson(id: Long, json: String)(implicit s: Session) {
    json.decodeOption[C] foreach { mapper ⇒
      val q = for {
        o ← this if o.id === id
      } yield o.uuid
      q.update(mapper.uuid)
    }
  }

  def syncFromJson(json: String)(implicit s: Session) {
    json.decodeEither[List[C]] match {
      case \/-(mappers) ⇒
        mappers foreach syncFromMapper
        deleteFromSync(mappers)
        Log.d(s"Successfully synchronized db '${name}' from json")
      case -\/(error) ⇒
        Log.e(s"Error decoding json from sync: ${error}")
        Log.e(json)
    }
  }

  def deleteFromSync(mappers: List[C])(implicit s: Session) {
    val present = for { o ← this } yield o.uuid
    val deleted = present.buildColl[Set] &~ mappers.uuids.toSet
    deleteByUuidsUnrecorded(deleted flatten)
  }

  def deleteByUuidsUnrecorded(uuids: Traversable[String])(implicit s: Session)
  {
    filter { _.uuid inSet(uuids) }.delete
  }

  def syncFromMapper(mapper: C)(implicit s: Session)

  def completeSync(a: Action[Long])(implicit s: Session) = {
    pending foreach { pa ⇒
      pa.deleteAdditions(List(a.id))
      pa.deleteUpdates(List(a.id))
    }
  }

  def completeDeletion(a: Deletion)(implicit s: Session) = {
    pending foreach { _.deleteDeletions(List(a.id)) }
  }
}
