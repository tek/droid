package tryp.droid.view

import android.preference.PreferenceFragment
import android.os.Bundle

class SettingsFragment
  extends PreferenceFragment
  with tryp.droid.view.FragmentBase
{
  override def onStart = super.onStart
  override def onStop = super.onStop
  override def onViewStateRestored(state: Bundle) = {
    super.onViewStateRestored(state)
  }
  override def onActivityCreated(state: Bundle) {
    super.onActivityCreated(state)
  }
  
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    addPreferencesFromResource(xmlId("user_preferences"))
  }
}
