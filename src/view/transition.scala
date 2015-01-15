package tryp.droid

import macroid.FullDsl._

import android.transitions.everywhere._

import tryp.droid.Macroid._

case class TrypTransition[A <: View](name: String, trans: Transition,
  view: Ui[A])
{
  trans.addTarget(name)

  def get = view <~ transitionName(name)
}

abstract class TrypTransitions
extends ActivityContexts
{
  def go(root: ViewGroup, view: View) {
    val s = new Scene(root, view)
    val set = new TransitionSet()
    transitions foreach { t ⇒ set.addTransition(t) }
    set.setOrdering(ordering)
    set.setDuration(duration)
    TransitionManager.go(s, set)
  }

  val transitions: Seq[Transition]
  val ordering = TransitionSet.ORDERING_TOGETHER
  val duration = 400
}
