package tryp.droid.meta

object Env
{
  var test = false
  var debug = false

  def release = !(test || debug)
}
