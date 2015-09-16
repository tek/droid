package tryp.droid

import scalaz._, Scalaz._

import android.widget._
import android.transitions.everywhere.TransitionSet
import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.FragmentBuilder

import tryp.droid.util.OS
import tryp.droid.res.{PrefixResourceNamespace}
import tryp.droid.Macroid._
import tryp.droid.tweaks.Recycler._

trait FragmentBase
extends Fragment
with Broadcast
with FragmentManagement
with TrypActivityAccess
with AkkaFragment
with Snackbars
with Transitions
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

  def trypActivity = {
    activity match {
      case a: TrypActivity ⇒ Option[TrypActivity](a)
      case _ ⇒ None
    }
  }

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
}

abstract class TrypFragment
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
with AppPreferences
with Fab
{
  implicit val mainFrag: FragmentManagement = this

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
    Log.w(s"Unhandled result in ${this.className}: ${data}")
  }
}

abstract class ShowFragment[A <: Model]
extends MainFragment
{
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    setupData()
  }

  private def setupData() {
    val stored = getArguments.getString(Keys.dataId, "")
    if (model.isEmpty) {
      Try(ObjectId(stored)) match {
        case Success(id) ⇒ initData(id)
        case Failure(_) ⇒
          Log.e(s"No dataId in arguments to show fragment '${name}'")
      }
    }
  }

  var model: Option[A] = None

  private def initData(id: ObjectId) {
    Future { model = fetchData(id) } map(Unit ⇒ update())
  }

  def fetchData(id: ObjectId): Option[A]

  def update() {
    model foreach { updateData(_).run }
    fetchDetails()
  }

  def fetchDetails() {}

  def updateData(m: A): Ui[Any]

  override def onViewStateRestored(state: Bundle) {
    super.onViewStateRestored(state)
    update()
  }
}

object ShowFragment
extends ActivityContexts
{
  def apply[A <: Model](model: A)(ctor: ⇒ ShowFragment[A])
  (implicit a: Activity) = {
    val inst = ctor
    inst.model = Some(model)
    FragmentBuilder(Ui(inst), new Bundle)
      .pass(Keys.dataId → model.id).factory.get
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
