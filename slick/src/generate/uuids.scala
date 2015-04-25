package slickmacros

trait Uuids
{
  def uuid: Option[String]
}

object Uuids
{
  implicit class `seq of Uuids`(seq: Seq[_ <: Uuids]) {
    def uuids = seq map(_.uuid)
  }
}
