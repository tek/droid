package tryp
package droid

import android.preference.PreferenceFragment

class SettingsFragment
extends PreferenceFragment
with FragmentBase
with DefaultStrategy
{
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    addPreferencesFromResource(res.xmlId("user_preferences"))
  }
}
