package tryp.droid

abstract trait CallbackMixin {
  implicit def context: Context
  def onCreate(state: Bundle)
  protected def onStart()
  protected def onStop()
  def onResume()
  def onPause()
}
