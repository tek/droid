package slick.schema

import slick.db._

trait Base
{
  def tableMap: Map[String, TableMetadata]
}

trait Timestamps

trait Part

trait Uuids

trait Sync
