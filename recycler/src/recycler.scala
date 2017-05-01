package tryp
package droid
package recycler

import iota.ViewTree

import view.io.recycler._
import state.MainViewMessages.{CreateMainView, SetMainTree}
import state.MainTree

trait RVTree
{
  def recycler: RecyclerView
}

abstract class RVCell[A <: RecyclerViewHolder, B, C <: RecyclerAdapter[A, B]]
extends ViewCell
{
  type CellTree <: AnyTree with RVTree
  type RVA = C

  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  case class Update(items: Seq[B])
  extends Message

  case class RecyclerData(adapter: RVA)
  extends CState

  case class SetAdapter(adapter: RVA)
  extends Message

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
    case SetAdapter(adapter) => {
      case s => stateWithAdapter(s, adapter) :: HNil
    }
    case MainTree(CellTree(tree)) => {
      case s @ ViewData(_, RecyclerData(adapter)) =>
        val io = tree.recycler >>- recyclerAdapter(adapter) >>- recyclerConf >>- recyclerLayout
        stateWithTree(s, tree, None) :: SetMainTree(tree).broadcast :: ContextIO(io.map(_ => NopMessage)).main :: HNil
    }
  }
}

case class RVMain(container: FrameLayout, recycler: RecyclerView)
extends ViewTree[FrameLayout]
with RVTree
{
  override def toString = "RVMain"
}

trait SimpleRV[A <: RecyclerViewHolder, B, C <: RecyclerAdapter[A, B]]
extends RVCell[A, B, C]
{
  type CellTree = RVMain

  def infMain = inflate[RVMain]
}

abstract class StringRV
extends SimpleRV[StringHolder, String, StringRA]
{
  lazy val adapter = conIO(StringRA(_))
}
