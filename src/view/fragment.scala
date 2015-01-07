package tryp.droid

import android.widget._

import macroid.FullDsl._

import tryp.droid.util.OS
import tryp.droid.util.FragmentCallbackMixin
import tryp.droid.res.{Layouts,LayoutAdapter,PrefixResourceNamespace}
import tryp.droid.Macroid._

trait FragmentBase
extends view.Basic
with Broadcast
with view.Fragments
with FragmentCallbackMixin
with TrypActivityAccess
with AkkaFragment
with view.Snackbars
{
  self: Fragment ⇒

  override implicit def activity = getActivity

  override def view = getView

  def trypActivity = {
    activity match {
      case a: TrypActivity ⇒ Option[TrypActivity](a)
      case _ ⇒ None
    }
  }

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
extends android.app.Fragment
with FragmentBase
{
  def layoutRes: Option[Int] = None
  def layoutName: Option[String] = None

  val name = fragmentClassName(getClass)

  implicit def resourceNamespace = PrefixResourceNamespace(name.snakeCase)

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    setupToolbar()
  }

  override def fragmentManager = getChildFragmentManager

  override def onViewStateRestored(state: Bundle) = {
    super.onViewStateRestored(state)
  }

  override def onActivityCreated(state: Bundle) {
    super.onActivityCreated(state)
  }

  override def onCreateView(
    inflater: LayoutInflater, container: ViewGroup, state: Bundle
  ): View =
  {
    layoutRes map { inflater.inflate(_, container, false) } getOrElse {
      getUi(macroidLayout(state))
    }
  }

  def macroidLayout(state: Bundle): Ui[View] = {
    layoutAdapter map { _.layout } getOrElse { Layouts.dummy }
  }

  lazy val layoutAdapter: Option[LayoutAdapter] = {
    Layouts.get(layoutName)
  }

  def setupToolbar() {
    setHasOptionsMenu(true)
  }

  def title = name
}

abstract class MainFragment
extends TrypFragment
{
  override def setupToolbar() {
    super.setupToolbar()
    core ! Messages.ToolbarTitle(title)
  }
}
