package tryp
package droid

import state._
import view._

import shapeless._

import android.widget.{BaseAdapter,TextView,Filterable,Filter}
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup.LayoutParams._

import ViewExports._

abstract class ListAdapter(implicit val activity: Activity)
extends BaseAdapter
with tryp.droid.Confirm
with ActivityContexts
with Macroid
with DefaultStrategy
{
  def items: Seq[AnyRef]

  override def getCount: Int = items.size

  override def getItem(position: Int): Object = items(position)

  override def getItemId(position: Int): Long = position
}

abstract class XMLListAdapter(implicit activity: Activity)
extends ListAdapter
{
  override def getView(pos: Int, oldView: View, parent: ViewGroup): View = {
    val view = if (oldView != null) oldView else newView
    setupView(view, pos, parent)
    view
  }

  protected def setupView(view: View, position: Int, parent: ViewGroup)

  protected def label(view: View, name: String) = {
    activity.textView(s"${prefix}_$name")
  }

  protected def setAttrs(view: View, item: Map[String, String]) {
    attrs.foreach(attr ⇒ {
      label(view, attr) foreach { _.setText(item(attr)) }
    })
  }

  protected def newView: View = {
    res.layoutId(layoutName)
      .map(name ⇒ activity.getLayoutInflater.inflate(name, null))
      .getOrElse(new View(activity))
  }

  protected def layoutName: String

  protected def prefix: String

  protected def attrs: List[String]

  import android.view.View

  protected def visible(state: Boolean): Int = {
    if (state) View.VISIBLE else View.GONE
  }
}

abstract class RecyclerAdapter[A <: RecyclerView.ViewHolder, B: ClassTag](
  implicit val activity: Activity
)
extends RecyclerView.Adapter[A]
with ActivityContexts
with tryp.droid.HasActivity
with Filterable
with AkkaAdapter
with DefaultStrategy
{
  def items: Seq[B]

  var visibleItems: Seq[B] = Seq()

  def getItemCount = visibleItems.length

  override def getItemId(position: Int) = position

  var currentFilter = ""

  def filter(constraint: String) = {
    currentFilter = constraint
    applyFilter
  }

  def applyFilter = Ui(getFilter.filter(currentFilter))

  def updateVisibleData(newItems: Seq[B]) {
    visibleItems = sort(newItems)
    notifyDataSetChanged()
    dataUpdated()
  }

  def sort(items: Seq[B]) = items

  def dataUpdated() {}

  lazy val getFilter = {
    new Filter {
      def publishResults(q: CharSequence, results: Filter.FilterResults) {
        results.values match {
          case v: Seq[B] ⇒
            Ui(updateVisibleData(v)).run
          case v ⇒ {
            Log.e(s"Error casting filtering results in ${this.className}")
          }
        }
      }

      def performFiltering(constraint: CharSequence) = {
        val values = items filter { filterItem(_, constraint) }
        new Filter.FilterResults tap { result ⇒
          result.count = values.length
          result.values = values
        }
      }
    }
  }

  def filterItem(item: B, constraint: CharSequence) = true
}

abstract class SimpleRecyclerAdapter[A <: RecyclerView.ViewHolder, B: ClassTag]
(implicit activity: Activity)
extends RecyclerAdapter[A, B]
{
  var simpleItems = Buffer[B]()

  def items = simpleItems

  def updateItems(newItems: Seq[B]) = {
    simpleItems = newItems.toBuffer
    applyFilter
  }
}

case class StringHolder(view: View, content: ViewStream[TextView])
extends RecyclerView.ViewHolder(view)

trait StringRecyclerAdapter
extends SimpleRecyclerAdapter[StringHolder, String]
with ExtViews
with FrameLayoutCombinators
{
  import iota._

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    val tv = w[TextView]
    val v = c[FrameLayout](
      l[FrameLayout](tv :: HNil) >>=
        lp[FrameLayout](MATCH_PARENT, MATCH_PARENT) >>= nopK >>=
          selectableFg
    )
    StringHolder(v.perform(), tv.v)
  }

  def onBindViewHolder(holder: StringHolder, position: Int) {
    items(position) foreach { s ⇒
      val io = holder.content >>= text[TextView](s)
      io.performMain()
    }
  }
}
