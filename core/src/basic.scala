package tryp
package droid

import tryp.droid.util.Threading
import tryp.droid._

trait HasContext
{
  implicit def context: Context

  implicit lazy val settings = implicitly[Settings]
}

trait Basic
extends HasContext
with ResourcesAccess
with Logging
{
  type IdTypes = Int with String with Id

  def asyncTask[A, B](task: ⇒ B)(callback: B ⇒ Unit) = {
    new android.os.AsyncTask[A, Unit, B] {
      override def doInBackground(args: A*) = task
      override def onPostExecute(result: B) { callback(result) }
    }.execute()
  }

  def thread(callback: ⇒ Unit) {
    Threading.thread(callback)
  }

  def systemService[A: ClassTag](name: String) = {
    context.getSystemService(name) match {
      case a: A ⇒ a
      case _ ⇒ {
        throw new ClassCastException(
          s"Wrong class for ${implicitly[ClassTag[A]].className}!"
        )
      }
    }
  }
}

trait TrypActivityAccess
extends HasActivity
{
  def trypActivity = {
    activity match {
      case a: TrypActivity ⇒ Option[TrypActivity](a)
      case _ ⇒ None
    }
  }
}
