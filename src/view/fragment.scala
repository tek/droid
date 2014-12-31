package tryp.droid

import android.content.Context
import android.view.{ View, ViewGroup, LayoutInflater }
import android.os.Bundle
import android.app.{Activity ⇒ AActivity, Fragment ⇒ AFragment}
import android.widget._

import macroid.Contexts
import macroid.FullDsl._
import macroid.Ui

import com.melnykov.fab.FloatingActionButton

import tryp.droid.util.OS
import tryp.droid.util.FragmentCallbackMixin
import tryp.droid.res.{Layouts,LayoutAdapter,PrefixResourceNamespace}
import tryp.droid.Macroid._
import tryp.droid.{Macroid ⇒ T}

trait FragmentBase
extends view.Basic
with Broadcast
with view.Fragments
with FragmentCallbackMixin
with TrypActivityAccess
with AkkaFragment
{
  self: AFragment ⇒

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

abstract class Fragment
extends android.app.Fragment
with FragmentBase
with Contexts[AFragment]
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

  // Create a wrapper layout containing:
  // * a floating action button, showing 'icon' and dispatching touch to
  //   'onClick'
  // * the View created by the second block arg.
  def fabCorner(icon: String)(onClick: ⇒ Unit)(content: ⇒ Ui[View]) =
  {
    RL()(
      content,
      fabUi(icon)(onClick) <~
        rlp(↧, ↦) <~
        margin(right = 16 dp, bottom = 48 dp)
    )
  }

  def fabBetween(icon: String, headerId: Id)(onClick: ⇒ Unit)(header: Ui[View],
    content: Ui[View]) =
  {
    RL()(
      header <~ headerId,
      content <~ rlp(below(headerId)),
      fabUi(icon)(onClick) <~
        rlp(↦, alignBottom(headerId)) <~
        margin(
          right = res.dimen("fab_margin_normal").toInt,
          bottom = res.dimen("fab_margin_normal_minus").toInt)
    )
  }

  def fabUi(icon: String)(onClick: ⇒ Unit) = {
    w[FloatingActionButton] <~
      image(icon) <~
      imageScale(ImageView.ScaleType.CENTER) <~
      T.Fab.colors("colorAccentStrong", "colorAccent") <~
      On.click { Ui(onClick) }
  }

  def setupToolbar() {
    setHasOptionsMenu(true)
    core ! Messages.ToolbarTitle(title)
  }

  def title = name
}
