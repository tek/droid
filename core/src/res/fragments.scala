package tryp.droid

import macroid.FullDsl._
import macroid.{ActivityContext,FragmentBuilder,FragmentManagerContext}

import tryp.droid.Macroid._

abstract class FragmentFactory[A <: Fragment: ClassTag]
extends ActivityContexts
{
  FragmentFactories.add(this)

  def name = tryp.droid.util.Strings.objectName2mixed(this)

  def create(implicit a: Activity) = {
    val ident = s"${name}Fragment"
    createImpl.framed(Id(ident), Tag(ident))
  }

  def createImpl(implicit a: Activity): FragmentBuilder[A]
}

object FragmentFactories
extends ActivityContexts
{
  val factories = MMap[String, FragmentFactory[_ <: Fragment]]()

  def apply(name: String)(implicit a: Activity) = {
    factories.get(name) map {
      try { _.create }
      catch {
        case e: Exception ⇒ if (Env.debug) {
          throw e
        }
        null
      }
      } getOrElse {
      Log.e(s"Could not instantiate fragment '${name.mkString}'")
      f[Fragment].framed(0, "invalid")
    }
  }

  def add(factory: FragmentFactory[_ <: Fragment]) {
    factories(factory.name) = factory
  }
}