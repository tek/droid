package tryp.droid

import tryp.droid.util.Threading
import tryp.droid.res._

trait HasContext
{
  implicit def context: Context
}

trait Basic
extends HasContext
{
  type IdTypes = Int with String with Id

  def res(implicit ns: ResourceNamespace = GlobalResourceNamespace) =
    Resources()

  def theme(implicit ns: ResourceNamespace = GlobalResourceNamespace) =
    res.theme

  def asyncTask[A, B](task: ⇒ B)(callback: (B) ⇒ Unit) = {
    new android.os.AsyncTask[A, Unit, B] {
      override def doInBackground(args: A*) = task
      override def onPostExecute(result: B) { callback(result) }
    }.execute()
  }

  def thread(callback: ⇒ Unit) {
    Threading.thread(callback)
  }
}

trait TrypActivityAccess {
  def trypActivity: Option[TrypActivity]
}
