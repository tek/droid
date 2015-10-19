package tryp.test

trait TrypTestActivity
extends tryp.TrypActivity
{ self: Activity ⇒

  def setPref(key: String, value: Any) {
    prefs.set(key, value)
    onSharedPreferenceChanged(prefs.prefs, key)
  }
}
