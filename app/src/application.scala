package tryp
package droid

import java.io.File

import android.content.pm.ApplicationInfo

import slick.DroidDbInfo
import core.ApplicationI

trait TrypApplication
extends ApplicationI { self: android.app.Application =>
  def isDebug = {
    (getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0
  }

  def setupEnv() = {
    if (isDebug) setEnv(tryp.core.DebugEnv)
  }

  def setupLog(name: String) = {
    tryp.core.Logs.log =
      if (TrypEnv.release) tryp.droid.InternalLog
      else if (TrypEnv.unittest) tryp.core.StdoutLog
      else tryp.droid.DebugLog
    AndroidLog.tag = name
  }

  def createTrypApp(name: String) {
    setupEnv()
    setupLog(name)
  }
}

trait Application
extends TrypApplication
{
  self: android.app.Application =>

  def name = "tryp"

  abstract override def onCreate() {
    createTrypApp(name)
    super.onCreate()
  }
}
