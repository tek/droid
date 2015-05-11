package slick.db

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.meta.MTable

trait TableMetadataBase
{
  def name: String

  def table: TableQuery[_ <: Table[_]]

  def ddl = table.ddl

  def empty(implicit session: JdbcBackend#SessionDef) =
    MTable.getTables(name).list.isEmpty
}

case class TableMetadata(name: String, table: TableQuery[_ <: Table[_]])
extends TableMetadataBase

case class SchemaMetadata
(tables: Map[String, _ <: TableMetadataBase])
{
  def createMissingTables()(implicit session: JdbcBackend#SessionDef) = {
    tables collect {
      case (name, table) if(table.empty) ⇒ table.ddl.create
    }
  }

  def dropAll()(implicit session: JdbcBackend#SessionDef) = {
    tables collect { case (name, table) ⇒ Try(table.ddl.drop) }
  }
}
