package tryp.droid.util

import android.content.Context
import android.os.Bundle

abstract trait CallbackMixin {
  implicit def context: Context
  def onCreate(state: Bundle)
  protected def onStart
  protected def onStop
  def onResume
  def onPause
}

abstract trait FragmentCallbackMixin
extends CallbackMixin
{
  def onViewStateRestored(state: Bundle)
  def onActivityCreated(state: Bundle)
}
