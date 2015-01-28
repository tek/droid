package tryp.droid.debug

trait DebugApplication
extends android.app.Application
{
  abstract override def onCreate() {
    Try(super.onCreate()) recover {
      case e: java.lang.NoClassDefFoundError ⇒ throw ProguardCacheError(e)
      case e: java.lang.NoSuchMethodError ⇒ throw ProguardCacheError(e)
      case e ⇒ throw e
    }
  }
}
