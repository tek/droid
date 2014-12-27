package tryp.droid

import android.preference.PreferenceFragment
import android.os.Bundle

class SettingsFragment
extends PreferenceFragment
with tryp.droid.Basic
{
  def context = getActivity
  
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    addPreferencesFromResource(xmlId("user_preferences"))
  }
}
