package tryp
package droid

import scalaz._, Scalaz._, effect.IO

trait ViewMetadata
{
  def desc: String
}

case class SimpleViewMetadata(desc: String)
extends ViewMetadata

final class ViewOps[A <: View: ClassTag](v: A)
(implicit res: Resources)
{
  val metaKey = res.id("view_metadata")

  def meta: ViewMetadata = {
    v.getTag(metaKey) match {
      case m: ViewMetadata ⇒ m
      case _ ⇒ inferMetadata
    }
  }

  def inferMetadata = {
    SimpleViewMetadata(v.className)
  }

  def storeMeta(meta: ViewMetadata) = {
    IO {
      v.setTag(metaKey, meta)
    }
  }
}

trait ToViewOps
{
  implicit def ToViewOps(v: View)(implicit res: Resources) = new ViewOps(v)
}

trait ViewInstances
extends ToViewOps
{
  implicit def viewShow(implicit res: Resources) =
    new Show[View]
    {
      override def show(v: View) = {
        val meta = v.meta
        s"${meta.desc} (${v.getId})"
      }
    }
}
