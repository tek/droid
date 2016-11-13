package tryp
package droid
package view

import android.widget._

import scalaz._, concurrent.Task

trait ViewMetadata
{
  def cls: String

  def printable = cls.blue
}

case class ClsVMD(cls: String)
extends ViewMetadata

trait NamedVMDI
extends ViewMetadata
{
  def desc: String

  override def printable = s"${cls.blue}[${desc.green}]"
}

case class NamedVMD(cls: String, desc: String)
extends NamedVMDI

case class ExtVMD(cls: String, desc: String, extra: Params)
extends NamedVMDI
{
  override def printable = s"${super.printable} $extra"
}

final class ViewOps[A <: View: ClassTag](v: A)
extends Logging
{
  def context = v.getContext

  def res = Resources.fromContext(context)

  lazy val metaKey = droid.res.R.id.view_metadata

  def meta: ViewMetadata = {
    v.getTag(metaKey) match {
      case m: ViewMetadata => m
      case _ => inferMetadata
    }
  }

  def inferMetadata = {
    ClsVMD(v.className)
  }

  def storeMeta(meta: ViewMetadata) = {
    v.setTag(metaKey, meta)
  }

  def desc(desc: String) =
    storeMeta(NamedVMD(v.className, desc))
}

trait ToViewOps
{
  implicit def ToViewOps[A <: View: ClassTag](v: A): ViewOps[A] =
    new ViewOps(v)
}

trait ViewInstances
extends ToViewOps
{
  def extraInfo(v: View): Option[String] = {
    Some(v) collect {
      case tv: TextView => tv.getText.toString
      case ll: LinearLayout => ll.getOrientation match {
        case LinearLayout.HORIZONTAL => "H"
        case _ => "V"
      }
    }
  }

  implicit def detailedViewShow =
    new Show[View]
    {
      override def show(v: View) = {
        val meta = v.meta
        val extra = extraInfo(v) | ""
        s"${meta.printable} ${extra.yellow}"
      }
    }
}

final class ViewGroupOps(vg: ViewGroup)
{
  def children: Seq[View] = {
    (0 until vg.getChildCount) map { i => vg.getChildAt(i) }
  }
}

trait ToViewGroupOps
{
  implicit def ToViewGroupOps(v: ViewGroup) = new ViewGroupOps(v)
}
