package tryp
package droid

import rx._

trait Theme extends ActivityBase
{ self: Preferences ⇒

  private var themeInitialized = false

  abstract override def onCreate(state: Bundle) {
    observeTheme
    themeInitialized = true
    super.onCreate(state)
  }

  lazy val currentTheme = prefs.string("theme", defaultTheme)

  lazy val observeTheme = Obs(currentTheme) {
    changeTheme(currentTheme(), themeInitialized)
  }

  def changeTheme(name: String, restart: Boolean)

  def defaultTheme: String
}
