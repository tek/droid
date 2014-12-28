package tryp.droid

import android.content.Context
import android.view.{ View, ViewGroup, LayoutInflater }
import android.os.Bundle
import android.app.{Activity ⇒ AActivity, Fragment ⇒ AFragment}
import android.widget._

import macroid.Contexts
import macroid.FullDsl._
import macroid.Ui

import com.shamanland.fab.FloatingActionButton

import tryp.droid.util.OS
import tryp.droid.util.FragmentCallbackMixin
import tryp.droid.res.{Layouts,LayoutAdapter}
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
  override implicit def activity = getActivity

  override def view = getView

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
with Contexts[AFragment]
{
  def layoutRes: Option[Int] = None
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

  def fab(icon: String)(onClick: ⇒ Unit)(content: ⇒ Ui[View]) = 
  {
    RL()(
      content,
      w[FloatingActionButton] <~
        image("ic_cart") <~
        rlp(↧, ↦) <~
        imageScale(ImageView.ScaleType.CENTER) <~
        margin(right = 16 dp, bottom = 48 dp) <~
        T.Fab.color("colorAccent") <~
        On.click {
          onClick
          Ui.nop
        }
    )
  }
}

class ListFragment
extends android.app.ListFragment
with FragmentBase
with Contexts[AFragment]
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
