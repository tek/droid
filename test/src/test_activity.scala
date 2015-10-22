package tryp
package test

trait TrypTestActivity
extends TrypActivity
with AuthStateMock
{ act: Akkativity ⇒

  def setPref(key: String, value: Any) {
    prefs.set(key, value)
    onSharedPreferenceChanged(prefs.prefs, key)
  }
}
