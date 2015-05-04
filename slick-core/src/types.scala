package slick

import scala.slick.driver.SQLiteDriver.simple._

import db._

trait Types
{
  type ExtModel = Model with Uuids with Timestamps
  type ExtTable[A <: ExtModel] = Table[A] with TableEx[A]
}
