package tryp
package droid
package unit

object RecyclerSpec
{
  def items = List("first", "second")

  import IOOperation.exports._

  class SpecAgent
  extends ActivityAgent
  {
    override lazy val viewMachine =
      new RecyclerViewMachine[StringRecyclerAdapter] {
        override def handle = "spec"

        lazy val adapter = conS(implicit c => new StringRecyclerAdapter {})

        def admit: Admission = {
          case AppState.ContentViewReady(_) => {
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
