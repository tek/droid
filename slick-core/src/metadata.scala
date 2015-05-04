package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.meta.MTable

class TableMetadataBase(name: String, table: TableQuery[_ <: Table[_]])
{
  def ddl = table.ddl

  def empty(implicit session: JdbcBackend#SessionDef) =
    MTable.getTables(name).list.isEmpty
}

case class TableMetadata(name: String, table: TableQuery[_ <: Table[_]])
extends TableMetadataBase(name, table)

case class SchemaMetadata
(tables: Map[String, _ <: TableMetadataBase])
{
  def createMissingTables()(implicit session: JdbcBackend#SessionDef) {
    tables foreach {
      case (name, table) if(table.empty) ⇒ table.ddl.create
    }
  }

  def dropAll()(implicit session: JdbcBackend#SessionDef) {
    tables foreach { case (name, table) ⇒ Try(table.ddl.drop) }
  }
}
