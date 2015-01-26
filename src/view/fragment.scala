package tryp.droid

import scala.concurrent.ExecutionContext.Implicits.global

import android.widget._
import android.transitions.everywhere.TransitionSet

import macroid.FullDsl._
import macroid.FragmentBuilder

import tryp.droid.util.OS
import tryp.droid.res.{Layouts,LayoutAdapter,PrefixResourceNamespace}
import tryp.droid.Macroid._

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

  def layoutName: Option[String] = None

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
  override def onCreateView(
    inflater: LayoutInflater, container: ViewGroup, state: Bundle
  ): View =
  {
    layoutRes map { inflater.inflate(_, container, false) } getOrElse {
      getUi(layout(state) <~ uiRoot)
    }
  }

  def layout(state: Bundle) = macroidLayout(state)

  def macroidLayout(state: Bundle): Ui[ViewGroup] = {
    layoutAdapter map { _.layout } getOrElse { Layouts.dummy }
  }

  lazy val layoutAdapter: Option[LayoutAdapter] = {
    Layouts.get(layoutName)
  }
}

case class CannotGoBack()
extends java.lang.RuntimeException

abstract class MainFragment
extends TrypFragment
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

  def showDetails(data: Any) {  }
}

abstract class ShowFragment[A <: TrypModel]
extends TrypFragment
{
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    setupData()
  }

  private def setupData() {
    val id = getArguments.getLong(Keys.dataId, 0)
    if (model.isEmpty)
      if (id <= 0)
        Log.e(s"No dataId in arguments to show fragment '${name}'")
      else
        initData(id)
  }

  var model: Option[A] = None

  private def initData(id: Long) {
    val f = Future { model = fetchData(id) } map(Unit ⇒ update())
  }

  def fetchData(id: Long): Option[A]

  def update() {
    model foreach { updateData(_).run }
  }

  def updateData(m: A): Ui[Any]

  override def onViewStateRestored(state: Bundle) {
    super.onViewStateRestored(state)
    update
  }
}

object ShowFragment
extends ActivityContexts
{
  def apply[A <: TrypModel](model: A)(ctor: ⇒ ShowFragment[A])
  (implicit a: Activity) = {
    val inst = ctor
    inst.model = Some(model)
    FragmentBuilder(Ui(inst), new Bundle)
      .pass(Keys.dataId → model.id).factory.get
  }
}
