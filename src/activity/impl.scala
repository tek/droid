package tryp.droid

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
{ self: Activity ⇒

  override implicit def activity = this

  override def view = getWindow.getDecorView.getRootView

  override def searcher = this

  override def defaultTheme = res.string("pref_theme_default")
}

abstract class TrypDefaultActivity
extends ActionBarActivity
with TrypActivity
with tryp.droid.Broadcast
with Akkativity
with HasNavigation
{
  override def onStart { super.onStart }
  override def onStop { super.onStop }
  override def onResume { super.onResume }
  override def onPostCreate(state: Bundle) { super.onPostCreate(state) }
}

abstract class TrypDrawerActivity
extends TrypDefaultActivity
with Drawer
{
}
