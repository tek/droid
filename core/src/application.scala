package tryp
package droid
package core

trait ApplicationI
{ self: android.app.Application =>

  def onCreate()
  protected def attachBaseContext(base: Context)
}
