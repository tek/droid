package tryp.droid.res

import android.widget.LinearLayout

import macroid.FullDsl._

import tryp.droid.{ActivityContexts,HasActivity}
import tryp.droid.Broadcast

class LayoutAdapter(val layout: Ui[ViewGroup])
{
}

object LayoutAdapter
{
  implicit def `Ui from LayoutAdapter option`(
    adapter: Option[LayoutAdapter]
  )(implicit a: Activity): Ui[ViewGroup] =
  {
    adapter map { _.layout } getOrElse Layouts.dummy.layout
  }

  implicit def `Ui from LayoutAdapter`(adapter: LayoutAdapter) = {
    adapter.layout
  }

  implicit def `LayoutAdapter from Ui`(layout: Ui[ViewGroup])(
    implicit activity: Activity
  ) = {
    new LayoutAdapter(layout)
  }
}

object Layouts
extends ActivityContexts
{
  abstract class Layout()
  extends HasActivity
  with ActivityContexts
  {
    Layouts.add(this)

    override implicit def activity: Activity = Layout.impAct

    private[Layouts] def create = createImpl

    protected def createImpl: LayoutAdapter

    protected def orientation = landscape ? horizontal | vertical

    protected def orientationInv = landscape ? vertical | horizontal
  }

  object Layout
  {
    implicit var impAct: Activity = null
  }

  val layouts = MMap[String, Layout]()

  def apply(name: String)(
    implicit a: Activity
  ): Option[LayoutAdapter] = {
    get(Option(name))
  }

  def get(name: Option[String])(
    implicit a: Activity
  ): Option[LayoutAdapter] =
  {
    Layout.impAct = a
    name flatMap { layouts.get(_) } map {
      try { _.create }
      catch {
        case e: Exception ⇒ if (Env.debug) {
          throw e
        }
        null
      }
      } orElse {
      Log.e(s"Could not instantiate layout '${name.mkString}'")
      Some(dummy)
    }
  }

  def add(factory: Layout) {
    val name = tryp.droid.util.Strings.objectName2mixed(factory)
    layouts(name) = factory
  }

  def dummy(implicit a: Activity): LayoutAdapter = {
    w[LinearLayout]
  }
}
