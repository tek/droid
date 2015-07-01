package slick

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import scalaz._, Scalaz._

import db._

trait RestClient
{
  def post(path: String, body: String = "{}"): \/[String, String]
  def put(path: String, body: String = "{}"): \/[String, String]
  def delete(path: String, body: String = "{}"): \/[String, String]
  def get(path: String, body: String = "{}"): \/[String, String]
}

case class AuthError(msg: String)
extends java.lang.Exception

trait BackendSync
{
  import PendingActionsSchema._

  def pendingActionsFor(name: String)(implicit s: Session) = {
    PendingActionSet.filter(_.model === name).firstOption
  }

  def apply(schema: SyncSchemaBase)(implicit s: Session) =
  {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      schema.syncMetadata.tables.values
        .filterNot { a ⇒ exclude.contains(a.table.path) }
        .foreach { meta ⇒
          pendingActionsFor(meta.pendingActionsKey) foreach { pending ⇒
            val sender = Sender(meta.table, pending)
            sender()
          }
          val fetcher = Fetcher(meta.table)
          fetcher()
        }
    }
  }

  trait Syncer
  {
    def table: SyncTableQueryBase

    def errorWrap[A](action: ⇒ \/[String, String])(callback: (String) ⇒ Unit) {
      Try(action) match {
        case Success(\/-(result)) ⇒ callback(result)
        case Success(-\/(err)) ⇒ error(err)
        case Failure(e @ AuthError(_)) ⇒ throw e
        case Failure(err) ⇒ error(err)
      }
    }

    def error(e: Any) {
      Log.e(s"Error during sync request: $e")
    }
  }

  case class Sender(table: SyncTableQueryBase, set: PendingActionSet)
  (implicit s: Session)
  extends Syncer
  {
    def apply() {
      additions()
      updates()
      deletions()
    }

    def additions() {
      withJson(set.additions) foreach {
        case (a @ Addition(target, _), data) ⇒
          errorWrap { rest.post(table.path, data) } { result ⇒
            table.completeSync(a)
          }
      }
    }

    def updates() {
      withJson(set.updates) foreach {
        case (u @ Update(target, _), data) ⇒
          errorWrap { rest.put(s"${table.path}/${target}", data) } { _ ⇒
            table.completeSync(u)
        }
      }
    }

    def deletions() {
      set.deletions foreach { d ⇒
        errorWrap { rest.delete(table.path, d.target.toString) } { _ ⇒
          table.completeDeletion(d)
        }
      }
    }

    def withJson(actions: Seq[Action]) = {
      actions.zip(table.jsonForIds(actions.targets))
    }
  }

  case class Fetcher(table: SyncTableQueryBase)(implicit s: Session)
  extends Syncer
  {
    def apply() {
      errorWrap { rest.get(table.path) } { table.syncFromJson }
    }
  }

  def rest: RestClient

  def exclude: List[String] = List()
}
