package tryp.droid.activity

import scala.reflect.ClassTag

import scala.collection.JavaConversions._

import android.app.{Activity,Fragment}
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.{MenuItem,Gravity}
import android.widget.FrameLayout
import android.preference.PreferenceManager
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarDrawerToggle,ActionBarActivity}
import android.support.v7.widget.{Toolbar ⇒ AToolbar}

import macroid.FullDsl._
import macroid.{Tweak,Contexts}

import tryp.droid.util.CallbackMixin
import tryp.droid.Macroid._
import tryp.droid.tweaks.{Toolbar ⇒ ToolbarT}
import tryp.droid.view.Fragments
import tryp.droid.SettingsFragment

trait ActivityBase
extends tryp.droid.view.HasActivity
with CallbackMixin
{
  def onConfigurationChanged(newConf: Configuration)
  def onOptionsItemSelected(item: MenuItem): Boolean
  protected def onPostCreate(state: Bundle)
  def onBackPressed()
}

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
  self: Activity
  with Fragments
  with Contexts[Activity] ⇒

  def setContentView(v: View)

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    initView
  }

  def initView = {
    setContentView(getUi(mainLayout))
  }

  def mainLayout = contentLayout

  def contentLayout: Ui[View] = {
    l[FrameLayout]() <~ Id.content <~ bgCol("main")
  }

  def loadContent[A <: Fragment: ClassTag](backStack: Boolean = true,
    title: String = "") = {
    replaceFragmentAuto[A](Id.content, backStack)
  }

  def loadContentCustom(fragment: Fragment, backStack: Boolean = true,
    title: String = "") = {
    replaceFragmentCustom(Id.content, fragment, backStack)
  }

  abstract override def onBackPressed() {
    !backStackEmpty ? popBackStackSync / super.onBackPressed()
  }
}

abstract trait Preferences
extends ActivityBase
with OnSharedPreferenceChangeListener
with tryp.droid.view.Preferences
{
  self: Activity
  with MainView
  with Fragments
  with Contexts[Activity] ⇒

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

  def settings() {
    loadContent[SettingsFragment](title = res.string("menu_settings"))
  }

  def setupPreferences {
    PreferenceManager.setDefaultValues(activity, res.xmlId("user_preferences"),
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
    val id = res.themeId(theme)
    if (id > 0) {
      applyTheme(id, restart)
    }
  }

  def applyTheme(id: Int, restart: Boolean = true) {
    activity.getApplicationContext.setTheme(id)
    activity.setTheme(id)
    if (restart) activity.recreate
  }

  def popSettings {
    popBackStackSync
  }
}

trait System
extends ActivityBase
{
  import android.view.View

  def hideStatusBar {
    activity.getWindow.getDecorView.setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_FULLSCREEN)
  }
}

trait Toolbar
extends MainView
{ self: ActionBarActivity
  with Fragments
  with Contexts[Activity] ⇒

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    toolbar foreach setSupportActionBar
  }

  val toolbar = slut[AToolbar]

  override def mainLayout = {
    l[FrameLayout](
      LL(vertical, llp(↔, ↕))(
        toolbarLayout,
        belowToolbarLayout
      )
    ) <~ fitsSystemWindows
  }

  def toolbarLayout = {
    inflateLayout[AToolbar]("toolbar") <~ ↔ <~ whore(toolbar)
  }

  def belowToolbarLayout: Ui[View] = contentLayout

  override def loadContent[A <: Fragment: ClassTag](backStack: Boolean = true,
    title: String = "")
  = {
    super.loadContent[A](backStack) tapIf {
      toolbarTitle(title)
    }
  }

  override def loadContentCustom(fragment: Fragment, backStack: Boolean = true,
    title: String = "")
  = {
    super.loadContentCustom(fragment, backStack) tapIf {
      toolbarTitle(title)
    }
  }

  def toolbarTitle(title: String) {
    runUi {
      toolbar <~
        ToolbarT.title(title.isEmpty ? res.string("app_title") / title)
    }
  }
}

trait Drawer
extends MainView
{ self: Activity
  with Fragments
  with Toolbar
  with Contexts[Activity] ⇒

  override def initView {
    super.initView
    initDrawer
  }

  def initDrawer {
    addFragment(Id.Drawer, drawerFragment, false, Tag.Drawer)
    drawerToggle foreach { toggle ⇒
      drawerSlot foreach {
        _.setDrawerListener(toggle)
      }
    }
  }

  val drawerSlot = slut[DrawerLayout]

  lazy val drawerToggle = {
    drawerSlot flatMap { drawer ⇒
      toolbar map { tb ⇒
        new ActionBarDrawerToggle(
          activity,
          drawer,
          tb,
          res.stringId("drawer_open"),
          res.stringId("drawer_close")
        )
      }
    }
  }

  override def belowToolbarLayout = {
    l[DrawerLayout](
      contentLayout,
      l[FrameLayout]() <~ Id.Drawer <~ dlp(res.dimen("drawer_width"), ↕)
    ) <~ whore(drawerSlot) <~ llp(↔, ↕)
  }

  abstract override def onPostCreate(state: Bundle) {
    super.onPostCreate(state)
    drawerToggle foreach(_.syncState)
  }

  abstract override def onConfigurationChanged(newConf: Configuration) {
    super.onConfigurationChanged(newConf)
    drawerToggle foreach { _.onConfigurationChanged(newConf) }
  }

  abstract override def onOptionsItemSelected(item: MenuItem) = {
    drawerToggle.exists(_.onOptionsItemSelected(item)) ||
      super.onOptionsItemSelected(item)
  }

  def closeDrawer() {
    drawerSlot foreach { _.closeDrawer(Gravity.LEFT) }
  }

  def drawerFragment: Fragment
}
