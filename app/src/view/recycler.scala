package tryp
package droid

import iota._

import state.AppState._
import state.ViewMachine._

object RecyclerViewMachineData
{
}

abstract class RecyclerViewMachine[A <: RecyclerViewAdapter[_]: ClassTag]
extends state.ViewMachine[FrameLayout]
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
    def main: Main
  }

  object RecyclerData
  {
    def unapply(a: RecyclerData) = Some((a.main, a.adapter))
  }

  case class RVData(main: Main, adapter: A)
  extends RecyclerData
  {
    def tree = Some(main)
    def view = main.container
  }

  import view.io.recycler._

  case class SetAdapter(adapter: A)
  extends Message

  case class Main(container: FrameLayout, recycler: RecyclerView)
  extends ViewTree[FrameLayout]
  {
    override def toString = "Main"
  }

  override def machinePrefix = super.machinePrefix :+ "recycler"

  val adapter: StreamIO[A, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  def recyclerLayout = linear

  override def dataWithTree(data: Data, tree: ViewTree[FrameLayout]) = {
    def fallback = super.dataWithTree(data, tree)
    data match {
      case RecyclerDataBase(adapter) =>
        tree match {
          case a: Main =>
            RVData(a, adapter)
          case _ => fallback
        }
        case _ => fallback
    }
  }

  protected def dataWithAdapter(data: Data, adapter: A): RecyclerDataBase =
    data match {
      case RVData(main, _) =>
        RVData(main, adapter)
      case ViewData(view, Some(tree: Main)) =>
        RVData(tree, adapter)
      case _ =>
        SimpleRecyclerDataBase(adapter)
    }

  override def extraAdmit = {
    case CreateContentView =>
      _ << CreateAdapter
    case CreateAdapter =>
      _ << adapter.map(SetAdapter(_).to(this)) <<
        inf[Main].map(ContentTree(_).to(this))
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
}
