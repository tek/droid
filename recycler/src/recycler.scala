package tryp
package droid
package recycler

import iota.ViewTree

import shapeless.{Typeable, ::}

import cats.data.Kleisli

import emm.{Emm, |:, Base}
import emm.compat.cats._

import tryp.state.annotation.cell

import view.io.recycler.{nopK, linear, recyclerAdapter}
import state.MainViewMessages.{CreateMainView, SetMainTree}

trait RVTree
{
  def recycler: RecyclerView
}

trait RVCellBase
extends ViewCellBase
{
  type Model
  type Holder <: RecyclerViewHolder
  type Adapter <: RecyclerAdapter[Holder, Model]

  case object CreateAdapter
  extends Message

  case object AdapterInstalled
  extends Message

  case object InsertAdapter
  extends Message

  case class RecyclerData(adapter: Adapter)
  extends CState

  case class SetAdapter(adapter: Adapter)
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
    case S(ViewData(main, _), c, e) => S(ViewData(main, data), c, e)
    case ViewData(main, _) => ViewData(main, data)
    case _ => data
  }

  def stateWithAdapter(adapter: Adapter) = stateWithRecyclerData(RecyclerData(adapter))
}

@cell
trait RVCell
extends ViewCell
with RVCellBase
{
  type CellTree <: AnyTree with RVTree

  case class Update(items: Seq[Model])
  extends Message

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

trait DefaultRV
extends RVCell
{
  type CellTree = RVMain

  def infMain = inflate[RVMain]

  def narrowTree(tree: state.AnyTree) = tree match {
    case t: RVMain => Some(t)
    case _ => None
  }
}

@cell
trait SimpleRVAdapterCell
extends RVCellBase
{
  type Element <: AnyTree

  type Holder = RVHolder[Element]
  type Adapter = SimpleRecyclerAdapter[Element, Model]

  def adapter: PartialFunction[CState, CIO[Adapter]]

  def trans: Transitions = {
    case CreateAdapter => {
      case s =>
        Emm[Option |: CIO |: Base, Adapter](adapter.lift(s))
          .map((a: Adapter) => SetAdapter(a))
          .run
          .map(a => a.runner :: HNil)
    }
  }
}

trait SimpleRV
extends SimpleRVAdapterCell
{
  def infElem: IO[Element, Context]

  val bind: (Element, Model) => Unit

  def adapter = { case _ => conIO(RA(infElem, bind)) }
}

trait RV
extends RVCell
with SimpleRV

trait CommRV
extends RVCell
with SimpleRVAdapterCell
{
  def infElem: IO[Element, Context]

  def bind(comm: Comm)(tree: Element, model: Model): Unit

  def adapter = { case C(comm) => conIO(RA(infElem, bind(comm))) }
}

trait StringRV
extends SimpleRVAdapterCell
with DefaultRV
{
  type Model = String
  type Element = StringElement

  def adapter = { case _ => conIO(StringRA(_)) }
}
