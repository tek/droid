package tryp
package droid

// import android.support.v7.app.ActionBarActivity
import android.widget._

import scalaz._, Scalaz._, concurrent._, stream._

// abstract trait TrypActivity
// extends Themes
// with ManagePreferences
// with MainView
// with Akkativity
// with HasNavigation
// with Snackbars
// {
//   override implicit def activity = this

//   override def defaultTheme = res.string("pref_theme_default").toOption
// }

// abstract class TrypDefaultActivity
// extends ActionBarActivity
// with TrypActivity
// {
//   override def onStart() { super.onStart() }
//   override def onStop() { super.onStop() }
//   override def onResume { super.onResume }
//   override def onPostCreate(state: Bundle) { super.onPostCreate(state) }
// }

// abstract class TrypDrawerActivity
// extends TrypDefaultActivity
// with Drawer
// {
//   self: HasContextAgent =>
// }

trait ViewActivity
extends FreeActivityAgent
with state.FreeViewAgent
// with Themes
// with Preferences
{
  override implicit def activity = this

  // override def defaultTheme = res.string("pref_theme_default").toOption

  def title = "ViewActivity"

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    // setContentView(safeView)
  }
}
