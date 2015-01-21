package tryp.droid

import android.widget.RelativeLayout
import android.view.Gravity

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

  class TrypTransitionSet
  extends ActivityContexts
  {
    val set = new TransitionSet

    def moveTransition = {
      (new TransitionSet) tap { s ⇒
        val cb = new ChangeBounds
        cb.setReparent(true)
        s.addTransition(cb)
        s.addTransition(new ChangeTransform)
        s.addTransition(new ChangeClipBounds)
        s.addTransition(new ChangeImageTransform)
      }
    }
  }

  case class FragmentTransition()
  {
    def go(root: ViewGroup, view: View) {
      val s = new Scene(root, view)
      val set = new TransitionSet
      transitions foreach(t ⇒ set.addTransition(t.set))
      set.setOrdering(TransitionSet.ORDERING_TOGETHER)
      TransitionManager.go(s, set)
    }

    def +=(trans: TrypTransitionSet) {
      transitions += trans
    }

    val transitions: Buffer[TrypTransitionSet] = Buffer()
  }

  class TrypTransitions
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

    val transitions: Buffer[Transition] = Buffer()
    val ordering = TransitionSet.ORDERING_TOGETHER
    val duration = 400
  }

  object CommonTransitions
  extends TrypTransitionSet
  {
    val slideTop = new Slide(Gravity.TOP)
    val fadeBottom = new Fade(Fade.OUT)

    def header(implicit a: Activity) =
      TrypTransition("header", slideTop, w[View])

    def content(implicit a: Activity) =
      TrypTransition("content", fadeBottom, w[View])

    set.addTransition(slideTop)
    set.addTransition(fadeBottom)
    slideTop.setDuration(300)
    fadeBottom.setDuration(400)
  }

  implicit class `Slot transition helper`[A <: ViewGroup](root: Slot[A])
  {
    def transitionTo(trans: FragmentTransition, view: Ui[View]) {
      root foreach { r ⇒
        trans.go(r, view.get)
      }
    }
  }
}
