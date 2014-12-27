package tryp.droid

import android.os.Bundle
import android.app.Activity
import android.support.v7.app.ActionBarActivity

import macroid.Contexts

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
  override def onPostCreate(state: Bundle) { super.onPostCreate(state) }
}

abstract class TrypDrawerActivity
extends TrypDefaultActivity
with Toolbar
with Drawer
with tryp.droid.view.Themes
{
}