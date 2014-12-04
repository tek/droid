package tryp.droid.util

object Env
{
  var test = false
  var debug = false

  def release = !(test || debug)
}
