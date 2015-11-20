package tryp
package droid

import scalaz._, Scalaz._, effect.IO

trait ViewMetadata
{
  def desc: String

  def extra: Params
}

case class SimpleViewMetadata(desc: String, extra: Params = Map())
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

trait LayoutMetadata
extends ViewMetadata

object LayoutMetadata
{
  def lin(implicit res: Resources) = LinearLayoutMetadata("vertical", None)
}

case class LinearLayoutMetadata(dir: String, detail: Option[String],
  extra: Params = Map())
extends LayoutMetadata
{
  def suf = detail some(s ⇒ s": $s") none("")

  def desc = s"$dir linear$suf"
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

object ViewInstances
extends ViewInstances
