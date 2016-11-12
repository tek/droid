package tryp
package droid

import iota._

import state.AppState._
import state.ViewMachine._

object RecyclerViewMachineData
{
}

trait RVTree
{
  def recycler: RecyclerView
}

case class RVMain(container: FrameLayout, recycler: RecyclerView)
extends ViewTree[FrameLayout]
with RVTree
{
  override def toString = "RVMain"
}

abstract class RVMachine
[
A <: RecyclerViewAdapter[_]: ClassTag,
B <: ViewTree[_ <: ViewGroup] with RVTree: ClassTag
]
extends state.TreeViewMachine[B]
{
  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  trait RecyclerDataBase
  extends Data
  {
    def adapter: A
  }

  object RecyclerDataBase
  {
    def unapply(a: RecyclerDataBase) = Some(a.adapter)
  }

  case class SimpleRecyclerDataBase(adapter: A)
  extends RecyclerDataBase

  trait RecyclerData
  extends RecyclerDataBase
  with ViewData
  {
    def main: B
  }

  object RecyclerData
  {
    def unapply(a: RecyclerData) = Some((a.main, a.adapter))
  }

  case class RVData(main: B, adapter: A)
  extends RecyclerData
  {
    def view = main
  }

  import view.io.recycler._

  case class SetAdapter(adapter: A)
  extends Message

  override def machinePrefix = super.machinePrefix :+ "recycler"

  val adapter: IOX[A, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  def recyclerLayout = linear

  override def dataWithTree(data: Data, tree: B) = {
    data match {
      case RecyclerDataBase(adapter) =>
        RVData(tree, adapter)
      case _ => super.dataWithTree(data, tree)
    }
  }

  protected def dataWithAdapter(data: Data, adapter: A): RecyclerDataBase =
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
      _ << adapter.map(SetAdapter(_).back) <<
        infMain.map(ContentTree(_).back)
    case SetAdapter(adapter) => {
      case S(s, d) =>
        S(s, dataWithAdapter(d, adapter))
    }
    case MainViewMessages.MainViewLoaded => {
      case s @ S(_, RVData(main, adapter)) =>
        val io = con(_ => main.recycler) >>- recyclerAdapter(adapter) >>-
          recyclerConf >>- recyclerLayout
        s << io.unitUi << AdapterInstalled
    }
  }

  override def internalAdmit = extraInternal orElse super.internalAdmit
}

abstract class SimpleRV[A <: RecyclerViewAdapter[_]: ClassTag]
extends RVMachine[A, RVMain]
{
  def infMain = inf[RVMain]
}
