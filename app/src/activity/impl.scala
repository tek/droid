package tryp
package droid

import view._

import android.support.v7.app.ActionBarActivity
import android.widget._

import scalaz._, Scalaz._, concurrent._, stream._

import macroid.Contexts

import Macroid._
import state._
import core._

abstract trait TrypActivity
extends Themes
with ManagePreferences
with MainView
with Akkativity
with HasNavigation
with Snackbars
{
  override implicit def activity = this

  override def defaultTheme = res.string("pref_theme_default").toOption
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
  self: HasContextAgent =>
}

trait ViewActivity
extends ActivityAgent
with ViewAgent
with Themes
with Preferences
with ExtViews
{
  override implicit def activity = this

  override def defaultTheme = res.string("pref_theme_default").toOption

  def title = "ViewActivity"

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    setContentView(safeView)
  }
}

class StateAppViewActivity
extends Activity
with Logging
with StateStrategy
{
  def stateApp = getApplication match {
    case a: StateApplication => Maybe.just(a)
    case _ => Maybe.empty
  }

  protected def mainViewTimeout = 5 seconds

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    implicit val ctx: Context = this
    Thread.sleep(1000)
    stateApp.map(
      _.setActivity(this)
        .map(_.reify.flatMap(iota.kestrel(setContentView(_))))
        .sideEffect(_.performMain())
        .infraRunFor("set main view", mainViewTimeout)
    )
  }
}
