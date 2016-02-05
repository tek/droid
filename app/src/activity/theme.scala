package tryp
package droid

import scalaz._, Scalaz._

import rx._

trait Themes
extends ActivityBase
{
  self: Preferences ⇒

    private var themeInitialized = false

    abstract override def onCreate(state: Bundle) {
      observeTheme
      themeInitialized = true
      super.onCreate(state)
    }

    lazy val prefTheme = prefs.string("theme")

    lazy val currentTheme = Rx {
      val t = prefTheme()
      (t != "").option(t) orElse defaultTheme
    }

    lazy val observeTheme = Obs(currentTheme) {
      currentTheme() foreach(changeTheme(_, themeInitialized))
    }

    def changeTheme(theme: String, restart: Boolean = true) = {
      res.themeId(theme) foreach(applyTheme(_, restart))
    }

    def applyTheme(id: Int, restart: Boolean = true) {
      getApplicationContext.setTheme(id)
      setTheme(id)
      if (restart) recreate()
    }

    def defaultTheme: Option[String]
}
