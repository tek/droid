package tryp
package droid

abstract trait CallbackMixin {
  implicit def context: android.content.Context
  def onCreate(state: android.os.Bundle)
  protected def onStart()
  protected def onStop()
  def onResume()
  def onPause()
}
