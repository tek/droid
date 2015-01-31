package tryp.droid

import scala.collection.mutable.{Set ⇒ MSet}
import scala.concurrent.ExecutionContext

import android.widget._
import android.view.Gravity

import macroid.FullDsl._

import android.transitions.everywhere._

import com.melnykov.fab.FloatingActionButton

import tryp.droid.Macroid._

class WidgetBase(transName: String)
{
  def tweak = transitionName(transName)
}

case class Widget[A <: View](view: Ui[A], transName: String,
  transition: Transition, duration: Long = 300)
(implicit a: Activity)
extends WidgetBase(transName)
{
  lazy val create = view <~ ui

  val ui = slut[A]

  lazy val get = create <~ tweak

  def <~[B <: Tweak[A]](t: B) = get <~ t

  def <~~[B <: Snail[A]](t: B)(implicit ec: ExecutionContext) = ui <~~ t
}

case class Layout( transName: String, transition: Transition,
  duration: Long = 300)
(implicit a: Activity)
extends WidgetBase(transName)

object TransitionSets
{
  val cache: MMap[String, TransitionSet] = MMap()
}

case class FragmentTransition()
{
  def go(root: ViewGroup, view: View) {
    val s = new Scene(root, view)
    val set = new TransitionSet
    transitions foreach set.addTransition
    set.setOrdering(TransitionSet.ORDERING_TOGETHER)
    TransitionManager.go(s, set)
  }

  def +=(trans: TransitionSet) {
    transitions += trans
  }

  def ++=(trans: Seq[TransitionSet]) {
    transitions ++= trans
  }

  val transitions: Buffer[TransitionSet] = Buffer()

  def clear() { transitions.clear() }
}

trait Transitions
extends HasActivity
{
  val uiRoot = slut[ViewGroup]

  def attachRoot(root: Ui[ViewGroup]) = {
    root <~ uiRoot
  }

  def transition(newView: Ui[View]) {
    uiRoot transitionTo(transitions, newView)
  }

  val transitions = FragmentTransition()

  implicit class `Slot transition helper`[A <: ViewGroup](root: Slot[A])
  {
    def transitionTo(trans: FragmentTransition, view: Ui[View]) {
      root foreach { r ⇒
        trans.go(r, view.get)
      }
    }
  }

  import macroid.CanTweak

  implicit def `View is tweakable with Widget`[A <: View, B <: WidgetBase] =
    new CanTweak[A, B, A] {
      def tweak(v: A, w: B) = v <~ w.tweak
    }

  def resetTransitions() {
    transitions.clear()
    transitions += CommonWidgets.transitions
  }

  implicit class `Transition operator`(t: Transition) {
    def ++(other: Transition) = {
      (new TransitionSet) tap { s ⇒
        s.addTransition(t)
        s.addTransition(other)
      }
    }
  }

  object CommonWidgets
  extends Widgets
  {
    val header = layout(FL(), "header", slideTop)

    val content = layout(FL(), "content", fade, duration = 400)

    val fab = widget(w[FloatingActionButton], "fab", move ++ slideRight,
      duration = 400)
  }
}

class Widgets(implicit a: Activity)
{
  val transitionSet = MSet[Transition]()

  private def addTransitionSet(transName: String, transition: Transition,
    duration: Long = 300)
  {
    transitionSet += transition
    transition.setDuration(duration)
    transition.addTarget(transName)
  }

  def widget[A <: View](view: Ui[A], transName: String, transition: Transition,
    duration: Long = 300) =
  {
    addTransitionSet(transName, transition, duration)
    Widget(view, transName, transition)
  }

  def layout[A <: ViewGroup](view: (Ui[View]*) ⇒ Ui[A], transName: String,
    transition: Transition, duration: Long = 300) =
  {
    addTransitionSet(transName, transition, duration)
    new Layout(transName, transition)
  }

  def slideTop = new Slide(Gravity.TOP)

  def slideBottom = new Slide(Gravity.BOTTOM)

  def slideRight = new Slide(Gravity.RIGHT)

  def slideLeft = new Slide(Gravity.LEFT)

  def fade = new Fade

  def move = {
    new TransitionSet {
      val cb = new ChangeBounds
      cb.setReparent(true)
      addTransition(cb)
      addTransition(new ChangeTransform)
      addTransition(new ChangeClipBounds)
      addTransition(new ChangeImageTransform)
    }
  }

  def transitions = new TransitionSet {
    transitionSet foreach(addTransition)
  }
}
