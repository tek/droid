package schema

import scala.slick.driver.SQLiteDriver.simple._
import scala.slick.jdbc.meta.MTable

case class TableMetadata(name: String, table: TableQuery[_ <: Table[_]])
{
  def ddl = table.ddl

  def empty(implicit s: Session) = MTable.getTables(name).list.isEmpty
}
