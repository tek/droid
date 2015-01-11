package tryp.droid.test

trait TrypTestActivity
extends tryp.droid.TrypActivity
{ self: Activity ⇒

  def setPref(key: String, value: Any) {
    prefs.set(key, value)
    onSharedPreferenceChanged(prefs.prefs, key)
  }
}
