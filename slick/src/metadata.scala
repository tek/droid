package slick.db

import scala.slick.driver.SQLiteDriver.simple._

import argonaut._, Argonaut._

import slick._

import PendingActionsSchema._

case class SyncTableMetadata(name: String,
  table: SyncTableQueryBase with TableQuery[_ <: Table[_]])
extends TableMetadataBase
{
  def pendingActionsKey = table.path
}

case class SyncSchemaMetadata
(tables: Map[String, _ <: SyncTableMetadata])
