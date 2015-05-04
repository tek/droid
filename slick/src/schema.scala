package slick

import scala.slick.driver.SQLiteDriver.simple._
import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation

trait SyncSchemaBase
extends SchemaBase
{
  def syncMetadata: db.SyncSchemaMetadata
}

class SyncSchema
extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro SyncSchemaMacros.create
}

trait Empty

class SyncSchemaMacros(ct: Context)
extends SchemaMacros(ct)
{
  import c.universe._

  override def queryBase(desc: ClsDesc) =
    desc.assoc ? super.queryBase(desc) / tq"Empty"

  override def queryType(desc: ClsDesc) =
    desc.assoc ? super.queryType(desc) /
      tq"SyncTableQuery[${desc.typeName}, ${desc.tableType}]"

  override def extra(implicit classes: List[ClsDesc]) = {
    List(
      q"import PendingActionsSchema._",
      q"val pendingActions = PendingActionsSchema.pendingActionSets",
      q"val pendingMetadata = PendingActionsSchema.metadata",
      q"val syncMetadata = ${syncMetadata}"
    )
  }

  def syncMetadata(implicit classes: List[ClsDesc]) = {
    val data = classes map { cls ⇒
      val name = cls.query
      val meta = q"""
      db.SyncTableMetadata[${cls.typeName}, ${cls.tableType}](
        ${name.toString}, $name
      )
      """
      (name.toString, meta)
    }
    q"db.SyncSchemaMetadata(Map(..$data))"
  }

  override val extraBases =
    List(tq"slick.SyncSchemaBase")
}
