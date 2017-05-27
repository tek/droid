package tryp
package droid
package recycler

import iota.ViewTree

import shapeless.{Typeable, ::}

import tryp.state.annotation.cell

import view.io.recycler.{nopK, linear, recyclerAdapter}
import state.MainViewMessages.{CreateMainView, SetMainTree}

trait RVTree
{
  def recycler: RecyclerView
}

trait RVCellBase[A <: RecyclerViewHolder, B, RVA <: RecyclerAdapter[A, B]]
extends ViewCellBase
{
  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  case object InsertAdapter
  extends Message

  case class RecyclerData(adapter: RVA)
  extends CState

  case class SetAdapter(adapter: RVA)
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
}

@cell
trait RVCell[A <: RecyclerViewHolder, B, RVA <: RecyclerAdapter[A, B]]
extends ViewCell
with RVCellBase[A, B, RVA]
{
  type CellTree <: AnyTree with RVTree

  case class Update(items: Seq[B])
  extends Message

  // def adapter: IO[RVA, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  def recyclerLayout = linear

  override def stateWithTree(state: CState, tree: CellTree, sub: Option[CState], extra: Option[CState]) = {
    val adapter = state match {
      case r @ RecyclerData(adapter) => Some(r)
      case _ => None
    }
    super.stateWithTree(state, tree, sub.orElse(adapter), extra)
  }

  def trans: Transitions = {
    case SetAdapter(adapter) => { case s => stateWithAdapter(adapter)(s) :: InsertAdapter :: HNil }
    case InsertAdapter => {
      case ViewData(tree, RecyclerData(a)) =>
        val io = tree.recycler >>- recyclerAdapter(a) >>- recyclerConf >>- recyclerLayout
        io.map(_ => AdapterInstalled).main :: HNil
    }
    case TreeInserted => CreateAdapter.local :: HNil
    case Update(items) => {
      case AdapterExtractor(a) =>
        a.updateItems(items).unitMain :: HNil
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
trait SimpleRV[Model, Element <: AnyTree, Adapter <: SimpleRecyclerAdapter[Element, Model]]
extends RVCell[Holder[Element], Model, Adapter]
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
extends SimpleRV[String, StringElement, StringRA]
{
  lazy val adapter = conIO(StringRA(_))
}
