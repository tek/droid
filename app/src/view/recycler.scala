package tryp
package droid

import state._
import state.core._
import view._
import view.core._

trait RecyclerViewMachineData[A <: RecyclerViewAdapter[_]]
{
  trait Recycler[B <: View]
  extends ViewMachine.Layout[B]
  {
    def adapter: A
  }

  object Recycler
  {
    def unapply[B <: View](o: Recycler[B]) =
      Some((o.layout, o.adapter))
  }

  case class RecyclerData[B <: View]
  (layout: StreamIO[B, Context], adapter: A)
  extends Recycler[B]

  case class AdapterReady(adapter: A)
  extends Message

  case object AdapterInstalled
  extends Message
}

trait RecyclerViewMachine[A <: RecyclerViewAdapter[_]]
extends ViewMachine
with RecyclerViewMachineData[A]
{
  import ViewMachine._

  import io.recycler._

  override def machinePrefix = super.machinePrefix :+ "recycler"

  val adapter: StreamIO[A, Context]

  def recyclerConf: CK[RecyclerView] = io.recycler.nopK

  lazy val recycler =
    w[RecyclerView] >>-
      recyclerConf >>-
      recyclerLayout

  def recyclerLayout = linear

  private[this] def assembled = {
    adapter.flatMap { a => recycler >>- recyclerAdapter(a) }
  }

  def layoutIO = l[FrameLayout](assembled)

  // def createAdapter: Transit = _ << act(a => adapter(a)).map(AdapterReady(_))

  // def installAdapter(a: A): Transit = {
  //   case S(s, Layout(l)) =>
  //     S(s, RecyclerData(l, a)) << (recycler.v >>- recyclerAdapter(a)).unit <<
  //       AdapterInstalled
  // }

  // def update = IOMainTask(IO(_ => adapter.notifyDataSetChanged()))
  // def update = mainIO(adapter.notifyDataSetChanged())
}
