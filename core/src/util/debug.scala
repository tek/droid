package tryp.droid.meta

object Debug {
  def rescued[A](callback: ⇒ A): A = {
    try {
      callback
    }
    catch {
      case e: Throwable ⇒
        var cause = e
        while (cause != null) {
          if (cause != e) {
            Log.e("Caused) by:")
          }
          Log.e(s"${cause.getClass.getSimpleName}: ${cause.getMessage}")
          e.getStackTrace foreach { f ⇒ Log.e(f.toString) }
          cause = cause.getCause
        }
      null.asInstanceOf[A]
    }
  }
}
