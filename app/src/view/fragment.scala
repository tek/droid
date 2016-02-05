package tryp
package droid

import view._

import scalaz._, Scalaz._, concurrent._, stream._

import argonaut._, Argonaut._

import android.widget._
import android.transitions.everywhere.TransitionSet
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater

import macroid.FullDsl._

import util.OS
import tweaks.Recycler._
import state._
import core._

trait FragmentBase
extends Fragment
with Broadcast
with TrypActivityAccess
with AkkaFragment
with Snackbars
with Transitions
with Macroid
with Screws
with DbAccess
with ResourcesAccess
with FragmentHelpers
{
  val name = fragmentClassName(getClass)

  def title = name

  def layoutRes: Option[Int] = None

  implicit def resourceNamespace = PrefixResourceNamespace(name.snakeCase)

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    setupToolbar()
  }

  def setupToolbar() {
    setHasOptionsMenu(true)
  }

  override implicit def activity = getActivity

  abstract override def onViewStateRestored(state: Bundle) {
    if (OS.hasFragmentOnViewStateRestored) {
      super.onViewStateRestored(state)
    }
  }

  abstract override def onActivityCreated(state: Bundle) {
    super.onActivityCreated(state)
    if (!OS.hasFragmentOnViewStateRestored) {
      onViewStateRestored(state)
    }
  }

  def arguments = Option(getArguments) some(_.toMap) none(Map())
}

trait TrypFragment
extends Fragment
with FragmentBase
with FragmentAgent
{
  lazy val viewMachine: ViewMachine = new DummyViewMachine {}

  override def onCreateView
  (inflater: LayoutInflater, container: ViewGroup, state: Bundle) = {
    Ui.get(layout(state) <~ uiRoot)
  }

  def layout(state: Bundle): Ui[ViewGroup]

  def resLayout(inflater: LayoutInflater, container: ViewGroup) = {
    layoutRes
      .some(id ⇒ Ui(inflater.inflate(id, container, false)))
      .none(RL()())
  }
}

trait VSTrypFragment
extends Fragment
with FragmentBase
with FragmentAgent
with ExtViews
{
  def dummyLayout = w[TextView] >>=
    iota.text[TextView]("Couldn't load content")

  def viewMachine: ViewMachine

  override def machines = viewMachine :: super.machines

  override def onCreateView
  (inflater: LayoutInflater, container: ViewGroup, state: Bundle) = {
    val l = (viewMachine.layout.discrete |> Process.await1)
      .runLast
      .unsafePerformSyncAttemptFor(10 seconds) match {
      case \/-(Some(l)) ⇒ l
      case \/-(None) ⇒
        log.error("no layout produced by ViewMachine")
        dummyLayout
      case -\/(error) ⇒
        log.error(s"error creating layout in ViewMachine: $error")
        dummyLayout
      }
    l.perform() unsafeTap { v ⇒
      log.debug(s"setting view for fragment $title:\n${v.viewTree.drawTree}")
    }
  }
}

case class CannotGoBack()
extends java.lang.RuntimeException

abstract class MainFragment
extends TrypFragment
with Fab
with AppPreferences
{
  override def onStart() {
    super.onStart()
    mainActor ! TrypActor.AttachUi(this)
  }

  override def onStop() {
    super.onStop()
    mainActor ! TrypActor.DetachUi(this)
  }

  override def onResume() {
    super.onResume()
    core ! Messages.ToolbarTitle(title)
  }

  def allTransitions: List[TransitionSet] = List()

  override def onViewStateRestored(state: Bundle) {
    super.onViewStateRestored(state)
    core ! Messages.Transitions(allTransitions)
  }

  def back(): Ui[Any] = {
    Ui { throw CannotGoBack() }
  }

  def dataLoaded() {}

  def result(data: Any) {
    Log.w(s"Unhandled result in ${this.className}: $data")
  }
}

abstract class ShowFragment[A <: Model]
extends MainFragment
{
  def showMachine: ShowMachine[A]

  override def machines = showMachine %:: super.machines

  override def onViewStateRestored(state: Bundle) {
    super.onViewStateRestored(state)
    send(Update)
  }
}

object ShowFragment
extends ActivityContexts
{
  def apply[A <: SyncModel](model: A)(ctor: ⇒ ShowFragment[A])
  (implicit a: Activity) = {
    val inst = ctor
    macroid.FragmentBuilder(Ui(inst), new Bundle)
      .pass(Keys.dataId → model.id, Keys.model → model.simpleJson.spaces2)
      .factory.get
  }
}

trait RecyclerFragment[A <: RecyclerView.Adapter[_]]
{
  self: FragmentBase ⇒

  def adapter: A

  def recyclerTweaks: Tweak[RecyclerView]

  val recyclerView = slut[RecyclerView]

  def layout(state: Bundle) = {
    w[RecyclerView] <~ recyclerAdapter(adapter) <~ recyclerTweaks <~
      recyclerView
  }

  def update() { Ui(adapter.notifyDataSetChanged()).run }
}
