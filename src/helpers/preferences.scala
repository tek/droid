package tryp.droid

import android.content.SharedPreferences
import android.preference.PreferenceManager

trait Preferences
extends HasContext
{
  def pref(key: String, default: String = "") = {
    prefs.getString(s"pref_${key}", default)
  }

  def prefBool(key: String, default: Boolean = true) = {
    prefs.getBoolean(s"pref_${key}", default)
  }

  def prefInt(key: String, default: Int = 0) = {
    Try {
      prefs.getString(s"pref_${key}", "").toInt
    } getOrElse default
  }

  def prefs = PreferenceManager.getDefaultSharedPreferences(context)

  def editPrefs(callback: (SharedPreferences.Editor) ⇒ Unit,
    target: SharedPreferences = prefs
  )
  {
    val editor = target.edit
    callback(editor)
    editor.commit
  }
}
