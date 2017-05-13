package tryp
package droid
package recycler

import iota.ViewTree

import tryp.state.annotation.cell

import view.io.recycler._
import state.MainViewMessages.{CreateMainView, SetMainTree}
import state.MainTree

trait RVTree
{
  def recycler: RecyclerView
}

case class SetAdapter(adapter: Any)
extends Message

object RVData
{
  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  case class Update(items: Seq[Any])
  extends Message

  case class RecyclerData(adapter: RecyclerAdapterI)
  extends CState
}

import RVData._

@cell
abstract class RVCell[A <: RecyclerViewHolder, B: ClassTag, RVA <: RecyclerAdapter[A, B]: ClassTag]
extends ViewCellBase
{
  type CellTree <: AnyTree with RVTree

  object AdapterExtractor
  {
    def unapply(s: CState) = s match {
      case RecyclerData(a) => Some(a)
      case ViewData(_, RecyclerData(a)) => Some(a)
      case _ => None
    }
  }

  def adapter: IO[RVA, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  def recyclerLayout = linear

  override def stateWithTree(state: CState, tree: CellTree, sub: Option[CState]) = {
    val adapter = state match {
      case r @ RecyclerData(adapter) => Some(r)
      case _ => None
    }
    super.stateWithTree(state, tree, sub.orElse(adapter))
  }

  def stateWithAdapter(state: CState, adapter: RVA): CState = {
    val data = RecyclerData(adapter)
    state match {
      case ViewData(main, _) => ViewData(main, data)
      case _ => data
    }
  }

  def trans: Transitions = {
    case CreateMainView =>
      ContextIO(adapter.map(SetAdapter(_))).broadcast :: ContextIO(infMain.map(MainTree(_))).broadcast :: HNil
    case SetAdapter(adapter: RVA) => {
      case s => stateWithAdapter(s, adapter) :: HNil
    }
    case MainTree(CellTree(tree)) => {
      case AdapterExtractor(a: RVA) =>
        val io = tree.recycler >>- recyclerAdapter(a) >>- recyclerConf >>- recyclerLayout
        stateWithTree(RecyclerData(a), tree, None) :: SetMainTree(tree).broadcast :: ContextIO(io.map(_ => NopMessage)).main.broadcast :: HNil
    }
    case Update(items: Seq[B]) => {
      case AdapterExtractor(a: RVA) =>
        ContextIO(a.updateItems(items).map(_ => NopMessage)).main.broadcast :: HNil
    }
  }
}

case class RVMain(container: FrameLayout, recycler: RecyclerView)
extends ViewTree[FrameLayout]
with RVTree
{
  override def toString = "RVMain"
}

@cell
abstract class SimpleRV[A <: RecyclerViewHolder, B: ClassTag, C <: RecyclerAdapter[A, B]: ClassTag]
extends RVCell[A, B, C]
{
  type CellTree = RVMain

  def infMain = inflate[RVMain]

  def narrowTree(tree: state.AnyTree) = tree match {
    case t: RVMain => Some(t)
    case _ => None
  }
}

@cell
trait StringRV
extends SimpleRV[StringHolder, String, StringRA]
{
  lazy val adapter = conIO(StringRA(_))
}
