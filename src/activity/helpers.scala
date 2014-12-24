package tryp.droid.activity

import scala.collection.JavaConversions._

import android.os.Bundle
import android.app.Activity
import android.content.SharedPreferences
import android.view.View
import android.preference.PreferenceManager
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.Context

import macroid.FullDsl._
import macroid.Ui

import tryp.droid.util.CallbackMixin
import tryp.droid.Macroid._

trait ActivityBase
extends tryp.droid.view.Activity
with CallbackMixin

trait Theme extends ActivityBase {
  def activity: Activity

  abstract override def onCreate(state: Bundle) {
    initTheme
    super.onCreate(state)
  }

  def initTheme {
    changeTheme(pref("theme", defaultTheme), false)
  }

  def changeTheme(name: String, restart: Boolean)

  def defaultTheme: String

  def pref(name: String, default: String = null): String
}

trait MainView
extends ActivityBase
{
  def setContentView(v: View)
  def layoutId(name: String): Int
  def mainLayout: Ui[View]

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    initView
  }

  def initView = {
    setContentView(getUi(mainLayout))
  }
}

abstract trait Preferences
extends ActivityBase
with OnSharedPreferenceChangeListener
with tryp.droid.view.Preferences
{
  self: Activity ⇒

  abstract override def onCreate(state: Bundle) {
    setupPreferences
    super.onCreate(state)
  }

  override def onSharedPreferenceChanged(prfs: SharedPreferences, key: String)
  {
    val value = prfs.getAll.get(key)
    val validated = validatePreference(key, value)
    if (validated != value) {
      editPrefs { editor ⇒
        value match {
          case s: String ⇒ editor.putString(key, s)
          case b: Boolean ⇒ editor.putBoolean(key, b)
          case _ ⇒
        }
      }
    }
    changePref(key, validated)
  }

  protected def validatePreference(key: String, value: Any): Any = {
    value
  }

  abstract override def onResume {
    super.onResume
    prefs.registerOnSharedPreferenceChangeListener(this)
  }

  abstract override def onPause {
    super.onPause
    prefs.unregisterOnSharedPreferenceChangeListener(this)
  }

  // TODO
  def settings {
    // inSettings = true
    // loadView new SettingsFragment
  }

  // override def onBackPressed {
    // if (inSettings) popSettings else super.onBackPressed
  // }

  def xmlId(name: String): Int

  def setupPreferences {
    PreferenceManager.setDefaultValues(activity, xmlId("user_preferences"),
      false)
  }

  def changePref(key: String, value: Any) {
    key match {
      case "pref_theme" ⇒ value match {
        case s: String ⇒ changeTheme(s)
        case _ ⇒ prefError(key, value)
      }
      case _ ⇒
    }
  }

  protected def prefError(key: String, value: Any) {
    Log.e("Invalid value for preference ${key}: ${value} (${value.getClass})")
  }

  // FIXME cannot resolve theme id in robolectric test
  def changeTheme(theme: String, restart: Boolean = true) {
    val id = themeId(theme)
    if (id > 0) {
      applyTheme(id, restart)
    }
  }

  def applyTheme(id: Int, restart: Boolean = true) {
    activity.getApplicationContext.setTheme(id)
    activity.setTheme(id)
    if (restart) activity.recreate
  }

  def themeId(name: String): Int

  // TODO
  def popSettings {
    // inSettings = false
    // fragmentManager.popBackStack
  }
}

trait System
extends ActivityBase
{
  def hideStatusBar {
    activity.getWindow.getDecorView.setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_FULLSCREEN)
  }
}
