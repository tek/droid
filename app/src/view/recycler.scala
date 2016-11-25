package tryp
package droid

import iota._

import state.AppState._
import state.TreeViewTrans

trait RVTree
{
  def recycler: RecyclerView
}

abstract class RVTrans[
A <: RecyclerViewHolder,
B,
C <: RecyclerAdapter[A, B],
D <: ViewTree[_ <: ViewGroup] with RVTree: ClassTag]
extends TreeViewTrans[D]
{
  type RVA = C

  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  case class Update(items: Seq[B])
  extends Message

  abstract class RecyclerDataBase
  extends Data
  {
    def adapter: RVA
  }

  object RecyclerDataBase
  {
    def unapply(a: RecyclerDataBase) = Some(a.adapter)
  }

  case class SimpleRecyclerDataBase(adapter: RVA)
  extends RecyclerDataBase

  abstract class RecyclerData
  extends ViewData
  {
    def adapter: RVA
    def main: D
  }

  object RecyclerData
  {
    def unapply(a: RecyclerData) = Some((a.main, a.adapter))
  }

  case class RVData(main: D, adapter: RVA)
  extends RecyclerData
  {
    def view = main
  }

  import view.io.recycler._

  case class SetAdapter(adapter: RVA)
  extends Message

  override def machinePrefix = super.machinePrefix :+ "recycler"

  val adapter: IOX[RVA, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  def recyclerLayout = linear

  override def dataWithTree(data: Data, tree: D) = {
    data match {
      case RecyclerDataBase(adapter) =>
        RVData(tree, adapter)
      case _ => super.dataWithTree(data, tree)
    }
  }

  protected def dataWithAdapter(data: Data, adapter: RVA): Data =
    data match {
      case RVData(main, _) =>
        RVData(main, adapter)
      case ViewData(tree) =>
        RVData(tree, adapter)
      case _ =>
        SimpleRecyclerDataBase(adapter)
    }

  protected def extraInternal: Admission = {
    case CreateContentView =>
      _ << CreateAdapter
    case CreateAdapter =>
      adapter.map(SetAdapter(_).back) << infMain.map(ContentTree(_).back)
    case SetAdapter(adapter) => {
      case S(s, d) =>
        S(s, dataWithAdapter(d, adapter))
    }
    case MainViewMessages.MainViewLoaded => {
      case S(_, RVData(main, adapter)) =>
        val io = con(_ => main.recycler) >>- recyclerAdapter(adapter) >>-
          recyclerConf >>- recyclerLayout
        io.unitUi << AdapterInstalled
    }
    case Update(items) => {
      case s @ S(_, RVData(main, adapter)) =>
        adapter.updateItems(items).unitUi
    }
  }

  override def internalAdmit = extraInternal orElse super.internalAdmit
}

case class RVMain(container: FrameLayout, recycler: RecyclerView)
extends ViewTree[FrameLayout]
with RVTree
{
  override def toString = "RVMain"
}

abstract class SimpleRV[A <: RecyclerViewHolder, B, C <: RecyclerAdapter[A, B]]
extends RVTrans[A, B, C, RVMain]
{
  def infMain = inf[RVMain]
}
