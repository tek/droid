package tryp.droid

import scala.concurrent.ExecutionContext.Implicits.global

import android.widget._

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
{

  val name = fragmentClassName(getClass)

  def title = name

  def layoutRes: Option[Int] = None

  def layoutName: Option[String] = None

  val uiRoot = slut[ViewGroup]

  implicit def resourceNamespace = PrefixResourceNamespace(name.snakeCase)

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    setupToolbar()
  }

  override def onCreateView(
    inflater: LayoutInflater, container: ViewGroup, state: Bundle
  ): View =
  {
    layoutRes map { inflater.inflate(_, container, false) } getOrElse {
      getUi(macroidLayout(state) <~ uiRoot)
    }
  }

  def macroidLayout(state: Bundle): Ui[ViewGroup] = {
    layoutAdapter map { _.layout } getOrElse { Layouts.dummy }
  }

  lazy val layoutAdapter: Option[LayoutAdapter] = {
    Layouts.get(layoutName)
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
}

abstract class MainFragment
extends TrypFragment
{
  override def onResume {
    super.onResume
    core ! Messages.ToolbarTitle(title)
  }
}

abstract class ShowFragment[A]
extends TrypFragment
{
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    setupData()
  }

  private def setupData() {
    val id = getArguments.getLong(Keys.dataId, 0)
    if (id > 0)
      initData(id)
    else
      Log.e(s"No dataId in arguments to show fragment '${name}'")
  }

  var model: Option[A] = None

  private def initData(id: Long) {
    val f = Future { model = fetchData(id) } map(Unit ⇒ update())
  }

  def fetchData(id: Long): Option[A]

  def update() {
    model foreach updateData
  }

  def updateData(m: A)

  override def onViewStateRestored(state: Bundle) {
    super.onViewStateRestored(state)
    update
  }
}

object ShowFragment
extends ActivityContexts
{
  def apply(id: Long)(ctor: ⇒ ShowFragment[_])(implicit a: Activity) = {
    FragmentBuilder(Ui(ctor), new Bundle).pass(Keys.dataId → id).factory.get
  }
}
