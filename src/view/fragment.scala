package tryp.droid

import android.content.Context
import android.view.{ View, ViewGroup, LayoutInflater }
import android.os.Bundle
import android.app.{Activity ⇒ AActivity}

import macroid.Contexts
import macroid.FullDsl.getUi
import macroid.Ui

import tryp.droid.util.OS
import tryp.droid.util.FragmentCallbackMixin
import tryp.droid.res.{Layouts,LayoutAdapter}
import tryp.droid.activity.TrypActivity

trait FragmentBase
extends tryp.droid.view.Basic
with tryp.droid.Broadcast
with tryp.droid.view.Fragments
with FragmentCallbackMixin
with tryp.droid.TrypActivityAccess
{
  override implicit def activity = getActivity

  override def view: View = getView

  def getActivity: AActivity

  def getView: View

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
with Contexts[android.app.Fragment]
{
  val layoutId: Option[Int] = None
  def layoutName: Option[String] = None

  override def onCreate(state: Bundle) = super.onCreate(state)
  override def onStart = super.onStart
  override def onStop = super.onStop

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
    layoutId map { inflater.inflate(_, container, false) } getOrElse {
      getUi(macroidLayout(state))
    }
  }

  def macroidLayout(state: Bundle): Ui[View] = {
    layoutAdapter map { _.layout } getOrElse { Layouts.dummy }
  }

  lazy val layoutAdapter: Option[LayoutAdapter] = {
    Layouts.get(layoutName)
  }
}

class ListFragment
  extends android.app.ListFragment
  with FragmentBase
{
  override def onCreate(state: Bundle) = super.onCreate(state)
  override def onStart = super.onStart
  override def onStop = super.onStop
  override def onViewStateRestored(state: Bundle) = {
    super.onViewStateRestored(state)
  }
  override def onActivityCreated(state: Bundle) {
    super.onActivityCreated(state)
  }

  override def fragmentManager = getChildFragmentManager
}
