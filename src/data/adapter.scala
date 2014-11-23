package tryp.droid

import scala.collection.mutable.ArrayBuffer

import android.widget.{BaseAdapter,TextView}
import android.view.{View,ViewGroup}
import android.app.Activity
import android.content.Context

import tryp.droid.util.view.AndroidExt._

abstract class ListAdapter(implicit val activity: Activity)
extends BaseAdapter
with tryp.droid.view.Confirm
{
  def items: ArrayBuffer[_ <: ListItemData]

  override def getCount: Int = items.size

  override def getItem(position: Int): Object = items(position)

  override def getItemId(position: Int): Long = position

  override def getView(pos: Int, oldView: View, parent: ViewGroup): View = {
    val view = if (oldView != null) oldView else newView
    setupView(view, pos, parent)
    view
  }

  protected def setupView(view: View, position: Int, parent: ViewGroup)

  protected def newView: View = {
    activity.getLayoutInflater.inflate(layoutId(layoutName), null)
  }

  protected def label(view: View, name: String) = {
    view.textView(s"${prefix}_${name}")
  }

  protected def setAttrs(view: View, item: Map[String, String]) {
    attrs.foreach(attr => {
      label(view, attr) foreach { _.setText(item(attr)) }
    })
  }

  protected def layoutName: String

  protected def prefix: String

  protected def attrs: List[String]

  protected def visible(state: Boolean): Int = {
    if (state) View.VISIBLE else View.GONE
  }
}
