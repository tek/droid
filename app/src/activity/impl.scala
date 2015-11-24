package tryp.droid

import java.util.concurrent.Executors

import android.support.v7.app.ActionBarActivity

import macroid.Contexts

import Macroid._

abstract trait TrypActivity
extends Themes
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

  override def defaultTheme = res.stringO("pref_theme_default")

  implicit lazy val ec =
    concurrent.ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))
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
  self: HasContextAgent ⇒
}
