package tryp.droid

import scala.language.postfixOps

import android.app.Activity
import android.view.{View,Gravity}
import android.widget.FrameLayout
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarActivity

import macroid.FullDsl._
import macroid.{Ui,Contexts}

import tryp.droid.activity._

import Macroid._

abstract trait TrypActivity
extends tryp.droid.view.Basic
with Theme
with tryp.droid.activity.Preferences
with MainView
with tryp.droid.view.Fragments
with Contexts[Activity]
{
  self: Activity ⇒

    override implicit def activity = this

    override def view = getWindow.getDecorView.getRootView

    override def defaultTheme = string("pref_theme_default")
}

abstract class TrypDefaultActivity
extends ActionBarActivity
with TrypActivity
with tryp.droid.Broadcast
with Akkativity
{
  override def onStart { super.onStart }
  override def onStop { super.onStop }
  override def onResume { super.onResume }
}

abstract class TrypDrawerActivity
extends TrypDefaultActivity
with Drawer
with Toolbar
{
  def drawerLayout = {
    l[FrameLayout](
      l[DrawerLayout](
        mainLayout,
        l[FrameLayout]() <~ Id.Drawer <~ dlp(dimen("drawer_width"), ↕)
      ) <~ whore(drawerSlot)
    ) <~ fitsSystemWindows <~ Id.drawerLayout
  }
}
