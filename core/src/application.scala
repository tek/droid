package tryp.droid

import android.content.pm.ApplicationInfo

trait TrypApplication
{ self: android.app.Application ⇒

  def createTrypApp(name: String) {
    if ((getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      Env.debug = true
    }
    tryp.util.Logs.log =
      if (Env.release) tryp.droid.meta.InternalLog
      else if (Env.unittest) tryp.meta.StdoutLog
      else tryp.droid.meta.DebugLog
    tryp.droid.meta.AndroidLog.tag = name
  }
}

class Application
extends android.app.Application
with TrypApplication
{
  def onCreate(name: String = "tryp") {
    super.onCreate()
    createTrypApp(name)
  }
}
