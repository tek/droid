package tryp
package droid
package unit

import state.core._
import state._

object RecyclerSpec
{
  def items = List("first", "second")

  import ViewStreamOperation.exports._

  class SpecAgent
  extends ActivityAgent
  {
    override lazy val viewMachine =
      new RecyclerViewMachine[StringRecyclerAdapter] {
        override def handle = "spec"

        lazy val adapter = conS(implicit c => new StringRecyclerAdapter {})

        override def extraAdmit = super.extraAdmit orElse {
          case ViewMachine.LayoutReady => {
            case s =>
              s << adapter.v.map(_.updateItems(items).ui)
          }
        }
      }
  }
}

trait RecyclerSpec
extends StateAppSpec
{
  import RecyclerSpec._

  override lazy val initialAgent = new SpecAgent
}

// class RecyclerEmptySpec
// extends RecyclerSpec
// {
//   import RecyclerSpec._

//   def is = s2"""
//   empty $empty
//   """

//   def empty = activity willContain emptyRecycler
// }

class RecyclerAddSpec
extends RecyclerSpec
{
  import RecyclerSpec._

  def is = s2"""
  add $add
  """

  def add = {
    activity willContain view[RecyclerView] and (
      activity willContain nonEmptyRecycler(2))
  }
}
