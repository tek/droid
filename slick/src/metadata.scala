package slick.db

import scala.slick.driver.SQLiteDriver.simple._

import slick._

import PendingActionsSchema._

abstract class SyncTableMetadataBase
(name: String, table: TableQuery[_ <: Table[_]])
extends TableMetadataBase(name, table)
{
  def sync(set: PendingActionSet)
}

case class SyncTableMetadata[A <: Types#ExtModel, B <: Types#ExtTable[A]]
(name: String, table: SyncTableQuery[A, B])
extends SyncTableMetadataBase(name, table)
{
  def sync(set: PendingActionSet) = ???
}

case class SyncSchemaMetadata
(tables: Map[String, _ <: SyncTableMetadataBase])
