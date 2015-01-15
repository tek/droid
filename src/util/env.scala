package tryp.droid.meta

object Env
{
  var test = false
  var unit = false
  var debug = false

  def release = !(test || debug)
  def unittest = test && unit
}
