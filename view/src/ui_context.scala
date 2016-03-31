package tryp
package droid
package view

import core._
import droid.core._

abstract class ContextAndroid[F[_, _]: ConsIO]
extends Android[F, Context]
with Logging
with ViewInstances
{
  implicit def context: Context

  def showViewTree(view: View): String = {
    view.viewTree.drawTree
  }

  override def notify(id: String): F[Unit, Context] = 
    ConsIO[F].pure(_ => log.info(id))

  def loadFragment(fragment: FragmentBuilder) = {
    ConsIO[F].pure(_ => "ContextAndroid cannot handle fragments")
  }

  def transitionFragment(fragment: FragmentBuilder) = {
    loadFragment(fragment)
  }

  def hideKeyboard() = 
    ConsIO[F].pure(_ => "Cannot hide keyboard without activity")

  def startActivity(cls: Class[_ <: Activity]): F[Int, Context] = {
    ConsIO[F].pure(_ => 1)
  }
}

class DefaultContextAndroid[F[_, _]: ConsIO]
(implicit val context: Context)
extends ContextAndroid[F]

object ContextAndroid
{
  def default[F[_, _]: ConsIO](implicit c: Context) = 
    new DefaultContextAndroid[F]
}
