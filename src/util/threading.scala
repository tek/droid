package tryp.droid.util

object Threading
{
  def thread(callback: => Unit) {
    val runnable = new Runnable() {
      def run = {
        try {
          callback
        } catch {
          case e: Exception => {
            Log.e("Error in thread:")
            Log.e(e.getStackTrace.mkString)
          }
        }
        callback
      }
    }
    new Thread(runnable).start()
  }
}
