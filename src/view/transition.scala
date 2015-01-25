package tryp.droid

import android.widget.RelativeLayout
import android.view.Gravity

import macroid.FullDsl._
import macroid.Tweak

import android.transitions.everywhere._

import com.melnykov.fab.FloatingActionButton

import tryp.droid.Macroid._

case class TrypTransition[A <: View](name: String, trans: Transition,
  view: Ui[A])
{
  trans.addTarget(name)

  def get = view <~ tweak

  def <~[B <: Tweak[A]](t: B) = get <~ t

  val tweak = transitionName(name)
}

class TrypTransitionSet
extends ActivityContexts
{
  val set = new TransitionSet

  def moveTransition = {
    new TransitionSet {
      val cb = new ChangeBounds
      cb.setReparent(true)
      addTransition(cb)
      addTransition(new ChangeTransform)
      addTransition(new ChangeClipBounds)
      addTransition(new ChangeImageTransform)
    }
  }
}

case class FragmentTransition()
{
  def go(root: ViewGroup, view: View) {
    val s = new Scene(root, view)
    val set = new TransitionSet
    // transitions.reverseIterator foreach(t ⇒ set.addTransition(t.set))
    transitions foreach(t ⇒ set.addTransition(t.set))
    set.setOrdering(TransitionSet.ORDERING_TOGETHER)
    TransitionManager.go(s, set)
  }

  def +=(trans: TrypTransitionSet) {
    transitions += trans
  }

  val transitions: Buffer[TrypTransitionSet] = Buffer()
}

object CommonTransitions
extends TrypTransitionSet
{
  val slideTop = new Slide(Gravity.TOP)
  val fadeBottom = new Fade
  val move = moveTransition

  def header(implicit a: Activity) =
    TrypTransition("header", slideTop, w[View])

  def content(implicit a: Activity) =
    TrypTransition("content", fadeBottom, w[View])

  def fab(implicit a: Activity) =
    TrypTransition("fab", move, w[FloatingActionButton])

  set.addTransition(slideTop)
  set.addTransition(fadeBottom)
  set.addTransition(move)
  slideTop.setDuration(300)
  fadeBottom.setDuration(400)
  move.setDuration(400)
}

trait Transitions
{
  val uiRoot = slut[ViewGroup]

  def attachRoot(root: Ui[ViewGroup]) = {
    root <~ uiRoot
  }

  def transition(newView: Ui[View]) {
    uiRoot transitionTo(transitions, newView)
  }

  val transitions = FragmentTransition()

  val defaultTransitions = CommonTransitions

  transitions += defaultTransitions

  implicit class `Slot transition helper`[A <: ViewGroup](root: Slot[A])
  {
    def transitionTo(trans: FragmentTransition, view: Ui[View]) {
      root foreach { r ⇒
        trans.go(r, view.get)
      }
    }
  }
}
