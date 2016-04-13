package tryp
package droid

import state._
import state.core._
import view._
import view.core._

trait RecyclerViewMachineData
{
}

trait RecyclerViewMachine[A <: RecyclerViewAdapter[_]]
extends ViewMachine
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

  def layout = l[FrameLayout](assembled)
}
