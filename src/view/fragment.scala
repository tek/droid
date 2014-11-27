package tryp.droid.view

import scala.reflect.ClassTag

import android.content.Context
import android.view.{ View, ViewGroup, LayoutInflater }
import android.os.Bundle
import android.app.{Activity ⇒ AActivity}

import macroid.Contexts
import macroid.FullDsl.getUi

import tryp.droid.util.OS
import tryp.droid.util.FragmentCallbackMixin
import tryp.droid.res.{Layouts,LayoutAdapter}

trait FragmentBase
extends tryp.droid.view.Basic
with tryp.droid.Broadcast
with FragmentCallbackMixin
{
  override implicit def activity = getActivity

  override def view: View = getView

  def getActivity: AActivity

  def getView: View

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

abstract class Fragment[A <: LayoutAdapter : ClassTag]
  extends android.app.Fragment
  with FragmentBase
  with Contexts[android.app.Fragment]
{
  val layoutId: Option[Int] = None
  def layoutName: Option[String] = None

  override def onCreate(state: Bundle) = super.onCreate(state)
  override def onStart = super.onStart
  override def onStop = super.onStop

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
    layoutId map { inflater.inflate(_, container, false) } orElse {
      layoutAdapter map { getUi(_) }
    } getOrElse { getUi(Layouts.dummy) }
  }

  lazy val layoutAdapter: Option[A] = {
    Layouts.get(layoutName) match {
      case l: Some[A] => l
      case l => {
        if (Env.debug) {
          throw new ClassCastException(
            s"Layout adapter type mismatch in ${getClass.getSimpleName}:" +
            s" Got ${l}"
          )
        }
        None
      }
    }
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
}
