package tryp
package droid

import scalaz._, Scalaz._

import argonaut._, Argonaut._

import android.widget._
import android.transitions.everywhere.TransitionSet
import android.support.v7.widget.RecyclerView

import macroid.FullDsl._

import util.OS
import tweaks.Recycler._
import ViewState._

trait FragmentBase
extends Fragment
with Broadcast
with FragmentManagement
with TrypActivityAccess
with AkkaFragment
with Snackbars
with Transitions
with Macroid
with Screws
with DbAccess
with ResourcesAccess
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

  override def view = getView

  override def fragmentManager = getChildFragmentManager

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
{

  override def onCreateView
  (inflater: LayoutInflater, container: ViewGroup, state: Bundle) = {
    Ui.get(layout(state) <~ uiRoot)
  }

  def layout(state: Bundle): Ui[ViewGroup]

  def resLayout(inflater: LayoutInflater, container: ViewGroup) = {
    layoutRes
      .some { id ⇒ Ui(inflater.inflate(id, container, false)) }
      .none { RL()() }
  }
}

case class CannotGoBack()
extends java.lang.RuntimeException

abstract class MainFragment
extends TrypFragment
with StatefulFragment
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
  def showImpl: ShowStateImpl[A]

  override def impls = showImpl :: super.impls

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
extends TrypFragment
{
  def adapter: A

  def recyclerTweaks: Tweak[RecyclerView]

  val recyclerView = slut[RecyclerView]

  def layout(state: Bundle) = {
    w[RecyclerView] <~ recyclerAdapter(adapter) <~ recyclerTweaks <~
      recyclerView
  }

  def update() { Ui(adapter.notifyDataSetChanged()).run }
}
