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
  self: HasContextAgent ⇒
}

trait ViewActivity
extends ActivityAgent
with Themes
with Preferences
with ExtViews
{
  override implicit def activity = this

  override def defaultTheme = res.string("pref_theme_default").toOption

  def title = "ViewActivity"

  def dummyLayout = w[TextView] >>=
    iota.text[TextView]("Couldn't load content")

  val viewMachine: ViewMachine

  override def machines = viewMachine :: super.machines

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    val l = (viewMachine.layout.discrete |> Process.await1)
      .runLast
      .unsafePerformSyncAttemptFor(10 seconds) match {
      case \/-(Some(l)) ⇒ l
      case \/-(None) ⇒
        log.error("no layout produced by ViewMachine")
        dummyLayout
      case -\/(error) ⇒
        log.error(s"error creating layout in ViewMachine: $error")
        dummyLayout
      }
    val view = l.perform() unsafeTap { v ⇒
      log.debug(s"setting view for fragment $title:\n${v.viewTree.drawTree}")
    }
    setContentView(view)
  }
}
