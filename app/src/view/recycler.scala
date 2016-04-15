package tryp
package droid

trait RecyclerViewMachineData
{
}

trait RecyclerViewMachine[A <: RecyclerViewAdapter[_]]
extends ViewMachine
{
  import view.io.recycler._

  override def machinePrefix = super.machinePrefix :+ "recycler"

  val adapter: StreamIO[A, Context]

  def recyclerConf: CK[RecyclerView] = nopK

  lazy val recycler =
    w[RecyclerView] >>-
      recyclerConf >>-
      recyclerLayout

  def recyclerLayout = linear

  private[this] def assembled = {
    adapter.flatMap { a => recycler >>- recyclerAdapter(a) }
  }

  lazy val layout: StreamIO[_ <: View, Context] = l[FrameLayout](assembled)
}
