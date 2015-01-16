package tryp.droid

import macroid.FullDsl._

import android.transitions.everywhere._

import tryp.droid.Macroid._

object Transitions
{

  case class TrypTransition[A <: View](name: String, trans: Transition,
    view: Ui[A])
  {
    trans.addTarget(name)

    def get = view <~ tweak

    val tweak = transitionName(name)
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

  implicit class `Slot transition helper`[A <: ViewGroup](root: Slot[A])
  {
    def transitionTo(trans: TrypTransitions, view: Ui[View]) {
      root foreach { r ⇒
        trans.go(r, view.get)
      }
    }
  }
}
