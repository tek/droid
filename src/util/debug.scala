package tryp.droid.util

object Debug {
  def rescued[A](callback: => A) = {
    try {
      callback
    }
    catch {
      case e: Throwable =>
        var cause = e
        while (cause != null) {
          if (cause != e) {
            (Log.p("Caused) by:"))
          }
          (Log.p(s"${cause.getClass.getSimpleName}: ${cause.getMessage}"))
          e.getStackTrace foreach { Log.p(_) }
          cause = cause.getCause
        }
    }
  }
}
