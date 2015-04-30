package schema

trait Schema
{
  def tableMap: Map[String, TableMetadata]
}

trait Timestamps

trait Part

trait Uuids
