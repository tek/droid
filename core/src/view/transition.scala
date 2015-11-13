package tryp.droid

import scala.collection.mutable.{Set ⇒ MSet}
import scala.concurrent.ExecutionContext

import android.widget._
import android.view.Gravity

import macroid._
import macroid.FullDsl._

import android.transitions.everywhere._

import com.melnykov.fab.FloatingActionButton

import tryp.droid.Macroid._
import tryp.droid.view.ParallaxHeader

class WidgetBase[A <: View](transName: String)
{
  val ui = slut[A]

  def tweak = transitionName(transName)

  def <~[B <: Tweak[A]](t: B) = ui <~ t
}

case class Widget[A <: View](view: Ui[A], transName: String,
  transition: Transition, duration: Long = 300)
(implicit a: Activity)
extends WidgetBase[A](transName)
{
  lazy val create = view <~ ui

  lazy val get = create <~ tweak

  def apply() = get

  def <~~[B <: Snail[A]](t: B)(implicit ec: ExecutionContext) = ui <~~ t
}

case class Layout[A <: ViewGroup](view: (Ui[View]*) ⇒ Ui[A], transName: String,
  transition: Transition, duration: Long = 300)
(implicit a: Activity)
extends WidgetBase[A](transName)
{
  def apply(children: Ui[View]*) = view(children: _*) <~ ui
}

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
    transitions.clear()
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

  def isEmpty = transitions.isEmpty
}

trait Transitions
extends HasActivity
with Macroid
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

  implicit def `View is tweakable with Widget`[A <: View, B <: Widget[A]] =
    new CanTweak[A, B, A] {
      def tweak(v: A, w: B) = v <~ w.tweak <~ w.ui
    }

  implicit def `VG is tweakable with Layout`[A <: ViewGroup, B <: Layout[A]] =
    new CanTweak[A, B, A] {
      def tweak(v: A, w: B) = v <~ w.tweak <~ w.ui
    }

  def addTransitions(set: Seq[TransitionSet]) {
    if (transitions.isEmpty)
      transitions += CommonWidgets.transitions
    transitions ++= set
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
    private def pxCtor(c: Ui[View]*) = l[ParallaxHeader](c: _*)

    val header = layout(pxCtor _, "header", slideTop)

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

  def widget[A <: View](view: Ui[A], transName: String = "widget",
    transition: Transition = new TransitionSet, duration: Long = 300) =
  {
    addTransitionSet(transName, transition, duration)
    Widget(view, transName, transition)
  }

  def layout[A <: ViewGroup](view: (Ui[View]*) ⇒ Ui[A], transName: String,
    transition: Transition, duration: Long = 300) =
  {
    addTransitionSet(transName, transition, duration)
    new Layout[A](view, transName, transition)
  }

  def slideTop = new Slide(Gravity.TOP)

  def slideBottom = new Slide(Gravity.BOTTOM)

  def slideRight = new Slide(Gravity.RIGHT)

  def slideLeft = new Slide(Gravity.LEFT)

  def fade = new Fade

  def move = {
    new TransitionSet {
      val cb = new ChangeBounds
      // cb.setReparent(true)
      addTransition(cb)
      addTransition(new ChangeTransform)
      addTransition(new ChangeClipBounds)
      addTransition(new ChangeImageTransform)
    }
  }

  def transitions = new TransitionSet {
    transitionSet foreach(addTransition)
  }

  def text(transName: String = "widget",
    transition: Transition = new TransitionSet, duration: Long = 300)
  (implicit a: macroid.ContextWrapper) =
  {
    widget(w[TextView], transName, transition, duration)
  }
}
