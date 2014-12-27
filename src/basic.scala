package tryp.droid

import android.content.Context
import android.content.res.Resources

import tryp.droid.util.Threading

trait Basic {
  type IdTypes = Int with String with util.Id

  implicit def context: Context

  def id[A >: IdTypes](input: A, defType: String = "id"): Int = {
    input match {
      case i: Int ⇒ i
      case i: util.Id ⇒ i
      case name: String ⇒ resources
        .getIdentifier(name, defType, context.getPackageName)
    }
  }

  def integer[A >: IdTypes](_id: A) = res(_id, "integer") { _.getInteger _ }

  def string[A >: IdTypes](_id: A) = res(_id, "string") { _.getString _ }

  def dimen[A >: IdTypes](_id: A) = res(_id, "dimen") { _.getDimension _ }

  def color[A >: IdTypes](_id: A) = res(_id, "color") { _.getColor _ }

  def xmlId(name: String): Int = id(name, "xml")

  def layoutId(name: String): Int = id(name, "layout")

  def themeId(name: String): Int = id(name, "style")

  def drawableId(name: String): Int = id(name, "drawable")

  def stringId(name: String): Int = id(name, "string")

  def resourceName(_id: Int) = resources.getResourceName(_id)

  def resources = context.getResources

  def res[A >: IdTypes, B](_id: A, defType: String)(
    callback: Resources ⇒ (Int ⇒ B)
  ): B = {
    try {
      callback(resources)(id(_id, defType))
    }
    catch {
      case e: Resources.NotFoundException ⇒ {
        val msg = s"No ${defType} with identifier '${_id}' found"
        throw new Resources.NotFoundException(msg)
      }
    }
  }

  def asyncTask[A, B](task: () ⇒ B)(callback: (B) ⇒ Unit) = {
    new android.os.AsyncTask[A, Unit, B] {
      override def doInBackground(args: A*) = task()
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
