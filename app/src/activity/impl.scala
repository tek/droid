package tryp.droid

import android.support.v7.app.ActionBarActivity

import macroid.Contexts

import Macroid._

abstract trait TrypActivity
extends Theme
with Broadcast
with ManagePreferences
with MainView
with FragmentManagement
with Akkativity
with HasNavigation
with Snackbars
{
  override implicit def activity = this

  override def view = getWindow.getDecorView.getRootView

  override def searcher = this

  override def defaultTheme = res.string("pref_theme_default")

  implicit def ec = scala.concurrent.ExecutionContext.Implicits.global
}

abstract class TrypDefaultActivity
extends ActionBarActivity
with TrypActivity
{
  override def onStart() { super.onStart() }
  override def onStop() { super.onStop() }
  override def onResume { super.onResume }
  override def onPostCreate(state: Bundle) { super.onPostCreate(state) }
}

abstract class TrypDrawerActivity
extends TrypDefaultActivity
with Drawer
{
  self: StatefulView ⇒
}
