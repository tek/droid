package slick

import scala.slick.driver.SQLiteDriver.simple._

import db._

trait Types
{
  type ExtModel[A] = Model with Sync with db.Timestamps[A]
  type ExtTable[A <: ExtModel[A]] = Table[A] with SyncTable[A]
}
