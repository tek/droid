package tryp.droid

import android.content.pm.ApplicationInfo

class Application extends android.app.Application
{
  def onCreate(name: String = "tryp") {
    super.onCreate
    if ((getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
      Env.debug = true
    }
    Log.tag = name
  }
}
