package tryp.droid

import android.preference.PreferenceFragment

case class SettingsFragment()
extends PreferenceFragment
with tryp.droid.Basic
{
  def context = getActivity
  
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    addPreferencesFromResource(res.xmlId("user_preferences"))
  }
}
