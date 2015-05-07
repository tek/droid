package slick

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import db._

trait HttpClient
{
  def post(path: String, body: String = "{}"): Option[String]
  def put(path: String, body: String = "{}"): Option[String]
  def delete(path: String, body: String = "{}"): Option[String]
  def get(path: String, body: String = "{}"): Option[String]
}

trait BackendSync
{
  import PendingActionsSchema._

  def apply(schema: SyncSchemaBase)(implicit s: Session) =
  {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      PendingActionSet.list foreach { set ⇒
        schema.syncMetadata.tables.get(set.model) foreach { meta ⇒
          val syncer = Syncer(meta.table, set)
          syncer.send()
          syncer.fetch()
        }
      }
    }
  }

  case class Syncer(table: SyncTableQueryBase, set: PendingActionSet)
  (implicit s: Session)
  {
    def errorWrap[A](action: ⇒ Option[String])(callback: (String) ⇒ Unit) {
      Try(action) match {
        case Success(Some(result)) ⇒
          callback(result)
        case Success(None) ⇒
          Log.e(s"No result in sync request")
        case Failure(e) ⇒
          Log.e(s"Error during sync request: $e")
        }
    }

    def send() {
      additions()
      updates()
      deletions()
    }

    def additions() {
      withJson(set.additions) foreach {
        case (a @ Addition(_, target), data) ⇒
          errorWrap { http.post(table.path, data) } { result ⇒
            table.setUuidFromJson(target, result)
            table.completeSync(a)
          }
      }
    }

    def updates() {
      withJson(set.updates) foreach {
        case (u @ Update(_, target), data) ⇒
          table.uuidById(target) foreach { uuid ⇒
            errorWrap { http.put(s"${table.path}/${uuid}", data) } { _ ⇒
              table.completeSync(u)
            }
          }
      }
    }

    def deletions() {
      set.deletions foreach { d ⇒
        errorWrap { http.delete(table.path, d.target) } { _ ⇒
          table.completeDeletion(d)
        }
      }
    }

    def fetch() {
      errorWrap { http.get(table.path) } { table.syncFromJson }
    }

    def withJson[A](actions: Seq[Action[Long]]) = {
      actions.zip(table.jsonForIds(actions.targets))
    }
  }

  def http: HttpClient
}
