package tryp.droid.debug

object `package`
extends tryp.droid.meta.TrypDroidGlobals

trait DebugApplication
extends tryp.droid.ApplicationI
{
  self: android.app.Application ⇒

  abstract override def onCreate() {
    Try(super.onCreate()) recover {
      case e: java.lang.NoClassDefFoundError ⇒ throw ProguardCacheError(e)
      case e: java.lang.NoSuchMethodError ⇒ throw ProguardCacheError(e)
      case e: java.lang.VerifyError ⇒ throw ProguardCacheError(e)
      case e ⇒ throw e
    }
  }
}
