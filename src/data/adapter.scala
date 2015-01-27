package tryp.droid

import android.widget.{BaseAdapter,TextView,Filterable,Filter}
import android.support.v7.widget.RecyclerView

abstract class ListAdapter(implicit val activity: Activity)
extends BaseAdapter
with tryp.droid.Confirm
with ActivityContexts
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
    view.textView(s"${prefix}_${name}")
  }

  protected def setAttrs(view: View, item: Map[String, String]) {
    attrs.foreach(attr ⇒ {
      label(view, attr) foreach { _.setText(item(attr)) }
    })
  }

  protected def newView: View = {
    activity.getLayoutInflater.inflate(res.layoutId(layoutName), null)
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
{
  def items: Seq[B]

  var visibleItems: Seq[B] = Seq()

  def getItemCount = visibleItems.length

  override def getItemId(position: Int) = position

  var currentFilter = ""

  def filter(constraint: String) {
    currentFilter = constraint
    applyFilter()
  }

  def applyFilter() {
    Ui(getFilter.filter(currentFilter)).run
  }

  def updateVisibleData(newItems: Seq[B]) {
    visibleItems = newItems
    notifyDataSetChanged
    dataUpdated()
  }

  def dataUpdated() {}

  lazy val getFilter = {
    new Filter {
      def publishResults(q: CharSequence, results: Filter.FilterResults) {
        results.values match {
          case v: Seq[B] ⇒ updateVisibleData(v)
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

  def updateItems(newItems: Seq[B]) {
    simpleItems = newItems.toBuffer
    applyFilter()
  }
}
