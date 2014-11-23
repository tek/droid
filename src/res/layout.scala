package tryp.droid.res

import scala.collection.mutable.{Map => MMap}

import android.app.{Activity => AActivity}
import android.view.View
import android.widget.LinearLayout
import scala.language.dynamics

import macroid.FullDsl._
import macroid.Ui

import tryp.droid.view.{ActivityContexts,Activity}

object Layouts
extends Dynamic
with ActivityContexts
{
  abstract class Layout()
  extends Activity
  {
    Layouts.add(this)

    override implicit def activity: AActivity = Layout.impAct

    private[Layouts] def create = createImpl

    protected def createImpl: Ui[View]

    protected def orientation = landscape ? horizontal | vertical

    protected def orientationInv = landscape ? vertical | horizontal
  }

  object Layout
  {
    implicit var impAct: AActivity = null
  }

  val layouts = MMap[String, Layout]()

  def selectDynamic(name: String)(implicit a: AActivity): Ui[View] = {
    get(Option(name))
  }

  def get(name: Option[String])(implicit a: AActivity): Ui[View] = {
    Layout.impAct = a
    name flatMap { layouts.get(_) } map {
      try { _.create }
      catch {
        case e: Exception => if (Env.debug) {
          throw e
        }
        null
      }
      } getOrElse {
      Log.e(s"Could not instantiate layout '${name}'")
      dummy
    }
  }

  def add(factory: Layout) {
    val name = tryp.droid.util.Strings.objectName2mixed(factory)
    layouts(name) = factory
  }

  def dummy(implicit a: AActivity) = {
    w[LinearLayout]
  }
}
