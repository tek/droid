package tryp.droid

import java.io.File

import android.content.pm.ApplicationInfo

import tryp.slick.DroidDbInfo

trait ApplicationI
{ self: android.app.Application ⇒

  def onCreate()
  protected def attachBaseContext(base: Context)
}

trait TrypApplication
extends HasContext
with ApplicationI
{ self: android.app.Application ⇒

  val useDb = true

  def setupDbInfo(name: String) = {
    val dbPath = new File(context.getFilesDir, s"$name.db")
    DbMeta.setDbInfo(DroidDbInfo(dbPath.toString))
  }

  def isDebug = {
    (getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0
  }

  def setupEnv() = {
    if (isDebug) setEnv(tryp.meta.DebugEnv)
  }

  def setupLog(name: String) = {
    tryp.util.Logs.log =
      if (TrypEnv.release) tryp.droid.meta.InternalLog
      else if (TrypEnv.unittest) tryp.meta.StdoutLog
      else tryp.droid.meta.DebugLog
    tryp.droid.meta.AndroidLog.tag = name
  }

  def createTrypApp(name: String) {
    setupEnv()
    setupLog(name)
    if (useDb) setupDbInfo(name)
  }
}

trait Application
extends TrypApplication
{
  self: android.app.Application ⇒

  def context = getApplicationContext

  def name: String

  abstract override def onCreate() {
    AndroidLog.d("droid Application")
    createTrypApp(name)
    super.onCreate()
  }
}
