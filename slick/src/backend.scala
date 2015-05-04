package slick

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend

import db._

trait HttpClient
{
  def post(path: String, body: String = "{}"): String = ???
  def put(path: String, body: String = "{}"): String = ???
  def delete(path: String, body: String = "{}"): String = ???
}

trait BackendSync
{
  def apply(schema: SyncSchemaBase)(implicit s: JdbcBackend#SessionDef) =
  {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      PendingActionsSchema.pendingActionSets.list foreach { set ⇒
        schema.syncMetadata.tables.get(set.model) foreach { meta ⇒
          meta.sync(set)
        }
      }
    }
  }

  def http: HttpClient
}
