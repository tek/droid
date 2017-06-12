package tryp
package droid
package unit

import tryp.state._
import state._

object RecyclerSpec
{
  def items = List("first", "second")

  import AIOOperation.exports._

  class SpecAgent
  extends ActivityAgent
  {
    override lazy val viewMachine =
      new RVMachine[StringRecyclerAdapter] {
        override def handle = "spec"

        lazy val adapter = conS(implicit c => new StringRecyclerAdapter {})

        def admit: Admission = {
          case state.AppState.ContentViewReady(_) => {
            case s =>
              s << adapter.v.map(_.updateItems(items).ui)
          }
        }
      }
  }
}

class RecyclerAddSpec
extends StateAppSpec
{
  import RecyclerSpec._

  def is = s2"""
  add $add
  """

  override lazy val initialAgent = new SpecAgent

  def add = {
    activity willContain emptyRecycler and (
      activity willContain nonEmptyRecycler(2))
  }
}
