package tryp
package droid
package view

import android.widget._

import scalaz._, concurrent.Task

trait ViewMetadata
{
  def desc: String

  def extra: Params
}

case class SimpleViewMetadata(desc: String, extra: Params = Map())
extends ViewMetadata

final class ViewOps[A <: View: ClassTag](v: A)
{
  lazy val metaKey = 12000

  def meta: ViewMetadata = {
    v.getTag(metaKey) match {
      case Right(m: ViewMetadata) => m
      case _ => inferMetadata
    }
  }

  def inferMetadata = {
    SimpleViewMetadata(v.className)
  }

  def storeMeta(meta: ViewMetadata) = {
    Task {
      v.setTag(metaKey, meta)
    }
  }

  def context = v.getContext

  def clickListen(callback: View => Unit) {
    v.setOnClickListener(new android.view.View.OnClickListener {
      def onClick(v: View) = callback(v)
    })
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
  def suf = detail.map(s => s": $s") | ""

  def desc = s"$dir linear$suf"
}

trait ToViewOps
{
  implicit def ToViewOps[A <: View: ClassTag](v: A): ViewOps[A] = 
    new ViewOps(v)
}

trait ViewInstances
extends ToViewOps
{
  def extraInfo(v: View) = {
    Some(v) collect {
      case tv: TextView => tv.getText
    }
  }

  implicit def detailedViewShow =
    new Show[View]
    {
      override def show(v: View) = {
        val meta = v.meta
        val extra = extraInfo(v) | ""
        s"${meta.desc} $extra (${v.getId})"
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
