package tryp
package droid
package recycler

import android.widget.{BaseAdapter,Filterable,Filter}
import android.view.ViewGroup.LayoutParams._

import iota.ViewTree

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

trait RecyclerAdapterI

trait RecyclerAdapter[A <: RecyclerViewHolder, B]
extends RecyclerViewAdapter[A]
// with Filterable
with Logging
{
  implicit def context: Context

  def items: Seq[B]

  def updateItems(newItems: Seq[B]): IO[Unit, Context]

  var visibleItems: Seq[B] = Seq()

  def getItemCount = visibleItems.length

  override def getItemId(position: Int) = position

  var currentFilter = ""

  // def filter(constraint: String) = {
  //   currentFilter = constraint
  //   applyFilter
  // }

  // def applyFilter = IO((c: Context) => getFilter.filter(currentFilter))

  def updateVisibleData(newItems: Seq[B]) {
    visibleItems = sort(newItems)
    notifyDataSetChanged()
  }

  def sort(items: Seq[B]) = items

  // lazy val getFilter = {
  //   new Filter {
  //     def publishResults(q: CharSequence, results: Filter.FilterResults) {
  //       results.values match {
  //         case v: Seq[B] =>
  //           IO((c: Context) => updateVisibleData(v))
  //             .main !? "update visible data"
  //         case v => {
  //           Log.e(s"Error casting filtering results in ${this.className}")
  //         }
  //       }
  //     }

  //     def performFiltering(constraint: CharSequence) = {
  //       val values = items filter { filterItem(_, constraint) }
  //       new Filter.FilterResults tap { result =>
  //         result.count = values.length
  //         result.values = values
  //       }
  //     }
  //   }
  // }

  def filterItem(item: B, constraint: CharSequence) = true
}

case class RVHolder[A <: AnyTree](tree: A)
extends RecyclerViewHolder(tree.container)

trait SimpleRecyclerAdapter[Tree <: AnyTree, Model]
extends RecyclerAdapter[RVHolder[Tree], Model]
with AnnotatedIO
with AnnotatedTIO
{
  def tree: IO[Tree, Context]

  def bindTree(tree: Tree, position: Int): Unit

  var simpleItems = Vector[Model]()

  def items = simpleItems

  def updateItems(newItems: Seq[Model]) = {
    conIO { _ =>
      simpleItems = newItems.toVector
      updateVisibleData(simpleItems)
    }
  }

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = RVHolder(tree(context))

  def onBindViewHolder(holder: RVHolder[Tree], position: Int) = bindTree(holder.tree, position)
}

trait RA[Tree <: AnyTree, Model]
extends SimpleRecyclerAdapter[Tree, Model]
{
  val bind: (Tree, Model) => Unit

  def bindTree(tree: Tree, position: Int): Unit = items.lift(position).foreach(bind(tree, _))
}

case class SimpleRA[Tree <: AnyTree, Model](tree: IO[Tree, Context], bind: (Tree, Model) => Unit, context: Context)
extends RA[Tree, Model]

object RA
{
  def apply[Tree <: AnyTree, Model](tree: IO[Tree, Context], bind: (Tree, Model) => Unit)(context: Context)
  : RA[Tree, Model] =
    SimpleRA(tree, bind, context)
}

case class StringElement(container: FrameLayout, label: TextView)
extends ViewTree[FrameLayout]
{
  container.matchWidth()
  label.matchWidth()
  override def toString = this.className
}

trait StringRecyclerAdapter
extends RA[StringElement, String]
{
  def tree = inflate[StringElement]

  val bind = (tree: StringElement, model: String) => tree.label.setText(model)
}

case class StringRA(context: Context)
extends StringRecyclerAdapter
