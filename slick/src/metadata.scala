package slick.db

import scala.slick.driver.SQLiteDriver.simple._

import argonaut._, Argonaut._

import slick._

import PendingActionsSchema._

trait SyncTableMetadataBase
extends TableMetadataBase
{
  // def sync(set: PendingActionSet)(implicit s: Session)

  def table: SyncTableQueryBase with TableQuery[_ <: Table[_]]
}

// FIXME parameterization superfluous, as all type specific action is done in
// SyncTableQuery
case class SyncTableMetadata[A <: Types#ExtModel,
B <: Types#ExtTable[A],
C <: BackendMapper[A]
]
(name: String, table: SyncTableQuery[A, B, C])
extends SyncTableMetadataBase
// {
//   def sync(set: PendingActionSet)(implicit s: Session) = {
//     import table.encodeJson
//     val objs = table.byIds(set.additions.targets)
//     println(objs.asJson.spaces2)
//   }
// }

case class SyncSchemaMetadata
(tables: Map[String, _ <: SyncTableMetadataBase])
