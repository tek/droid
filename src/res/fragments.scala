package tryp.droid.res

import scala.collection.mutable.{Map => MMap}
import scala.reflect.ClassTag

import android.view.View
import android.app.{Fragment,Activity,FragmentManager}
import android.widget.FrameLayout

import macroid.FullDsl._
import macroid.{Ui,ActivityContext,FragmentBuilder,FragmentManagerContext}

import tryp.droid.view.ActivityContexts
import tryp.droid.Broadcast
import tryp.droid.util.{Id,Tag}

abstract class FragmentFactory[A <: Fragment]
extends ActivityContexts
{
  Fragments.add(this)

  def name = tryp.droid.util.Strings.objectName2mixed(this)

  def create(implicit a: Activity) = {
    val ident = s"${name}Fragment"
    createImpl.framed(Id(ident), Tag(ident))
  }

  def createImpl(implicit a: Activity): FragmentBuilder[A]
}

object Fragments
extends ActivityContexts
{
  val factories = MMap[String, FragmentFactory[_]]()

  def apply(name: String)(implicit a: Activity): Ui[FrameLayout] = {
    factories.get(name) map {
      try { _.create }
      catch {
        case e: Exception => if (Env.debug) {
          throw e
        }
        null
      }
      } getOrElse {
      Log.e(s"Could not instantiate fragment '${name.mkString}'")
      f[Fragment].framed(0, "invalid")
    }
  }

  def add(factory: FragmentFactory[_]) {
    factories(factory.name) = factory
  }
}