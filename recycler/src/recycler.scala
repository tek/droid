package tryp
package droid
package recycler

import iota.ViewTree

import shapeless.{Typeable, ::}

import tryp.state.annotation.cell

import view.io.recycler._
import state.MainViewMessages.{CreateMainView, SetMainTree}

trait RVTree
{
  def recycler: RecyclerView
}

case class SetAdapter[A](adapter: A)
extends Message

object RVData
{
  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  case class RecyclerData(adapter: RecyclerAdapterI)
  extends CState
}

import RVData._

trait RVCellBase[A <: RecyclerViewHolder, B, RVA <: RecyclerAdapter[A, B]]
extends ViewCellBase
{
  case object InsertAdapter
  extends Message

  object AdapterExtractor
  {
    def unapply(s: CState) = s match {
      case RecyclerData(a) => Some(a)
      case ViewData(_, RecyclerData(a)) => Some(a)
      case _ => None
    }
  }

  def stateWithRecyclerData(data: RecyclerData): CState => CState = {
    case ViewData(main, _) => ViewData(main, data)
    case _ => data
  }

  def stateWithAdapter(adapter: RVA) = stateWithRecyclerData(RecyclerData(adapter))

  def setAdapter(s: CState, adapter: Any)(implicit ct: ClassTag[RVA]) = {
    adapter match {
      case a: RVA => Some(stateWithAdapter(a)(s) :: InsertAdapter :: HNil)
      case _ => None
    }
  }
}

@cell
abstract class RVCell[A <: RecyclerViewHolder, B: ClassTag, RVA <: RecyclerAdapter[A, B]: ClassTag]
extends ViewCell
with RVCellBase[A, B, RVA]
{
  type CellTree <: AnyTree with RVTree

  case class Update(items: Seq[B])
  extends Message

  def adapter: IO[RVA, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  def recyclerLayout = linear

  override def stateWithTree(state: CState, tree: CellTree, sub: Option[CState], extra: Option[CState]) = {
    val adapter = state match {
      case r @ RecyclerData(adapter) => Some(r)
      case _ => None
    }
    super.stateWithTree(state, tree, sub.orElse(adapter), extra)
  }

  def update(state: CState, items: Seq[B]): Option[ContextIO] =
    state match {
      case ViewData(v, RecyclerData(a: RVA)) =>
        dbg(v.recycler.hashCode)
        dbg(v.recycler.getAdapter.hashCode)
        dbg(a.hashCode)
        Some(ContextIO(a.updateItems(items).map(_ => NopMessage)).main)
      case _ => None
    }

  def trans: Transitions = {
    case SetAdapter(adapter) => { case s => setAdapter(s, adapter) }
    case InsertAdapter => {
      case ViewData(tree, RecyclerData(a: RVA)) =>
        val io = tree.recycler >>- recyclerAdapter(a) >>- recyclerConf >>- recyclerLayout
        ContextIO(io.map(_ => AdapterInstalled)).main :: HNil
    }
    case TreeInserted => {
      case ViewData(tree, _) =>
        ContextIO(adapter.map(SetAdapter(_))) :: HNil
    }
    case Update(items) => {
      case AdapterExtractor(a: RVA) =>
        ContextIO(a.updateItems(items).map(_ => NopMessage)).main :: HNil
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
