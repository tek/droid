package tryp
package droid

import android.widget.{BaseAdapter,Filterable,Filter}
import android.view.ViewGroup.LayoutParams._

abstract class ListAdapter(implicit val activity: Activity)
extends BaseAdapter
with DefaultStrategy
with ResourcesAccess
with Logging
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
    attrs.foreach(attr => {
      label(view, attr) foreach { _.setText(item(attr)) }
    })
  }

  protected def newView: View = {
    res.layoutId(layoutName)
      .map(name => activity.getLayoutInflater.inflate(name, null))
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

abstract class RecyclerAdapter[A <: RecyclerViewHolder, B: ClassTag]
extends RecyclerViewAdapter[A]
with Filterable
with Logging
{
  implicit def context: Context

  implicit def scheduler: Scheduler

  def items: Seq[B]

  def updateItems(newItems: Seq[B]): IOX[Unit, Context]

  var visibleItems: Seq[B] = Seq()

  def getItemCount = visibleItems.length

  override def getItemId(position: Int) = position

  var currentFilter = ""

  def filter(constraint: String) = {
    currentFilter = constraint
    applyFilter
  }

  def applyFilter = IO((c: Context) => getFilter.filter(currentFilter))

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
          case v: Seq[B] =>
            IO((c: Context) => updateVisibleData(v))
              .main !? "update visible data"
          case v => {
            Log.e(s"Error casting filtering results in ${this.className}")
          }
        }
      }

      def performFiltering(constraint: CharSequence) = {
        val values = items filter { filterItem(_, constraint) }
        new Filter.FilterResults tap { result =>
          result.count = values.length
          result.values = values
        }
      }
    }
  }

  def filterItem(item: B, constraint: CharSequence) = true
}

abstract class SimpleRecyclerAdapter[A <: RecyclerViewHolder, B: ClassTag]
extends RecyclerAdapter[A, B]
with AnnotatedIO
{
  var simpleItems = Vector[B]()

  def items = simpleItems

  def updateItems(newItems: Seq[B]) = {
    con { _ =>
      simpleItems = newItems.toVector
      // applyFilter
      updateVisibleData(simpleItems)
    }
  }
}

case class StringHolder(view: View, content: ViewStream[TextView, Context])
extends RecyclerViewHolder(view)

trait StringRecyclerAdapter
extends SimpleRecyclerAdapter[StringHolder, String]
with Views[Context, StreamIO]
{
  import iota.effect._
  import view.io.frame._
  import view.io.text._

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    val tv = w[TextView]
    val lay = l[FrameLayout](tv)
    val v = c[FrameLayout](
      (lay >>= lp[FrameLayout](MATCH_PARENT, MATCH_PARENT)) >>-
        selectableFg
    )
    val layout =
      v.unsafePerformIO !? "create layout for string recycler item" getOrElse(
        new FrameLayout(context))
    StringHolder(layout, tv.v)
  }

  def onBindViewHolder(holder: StringHolder, position: Int) {
    items.lift(position) foreach { s =>
      val io = holder.content >>- text(s)
      io.view
        .flatMap(a => Stream.eval(a.main))
        .run
        .infraRun("bind view holder")
    }
  }
}
