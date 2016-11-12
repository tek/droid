package tryp
package droid
package core

import scalaz._, scalaz.syntax.show._, scalaz.syntax.nel._
import scalaz.syntax.traverse._

import tryp.UiContext

case class FragmentBuilder(ctor: () => Fragment, id: RId,
  tagO: Option[String] = None)
  {
    def apply() = ctor()

    def tag = tagO | id.tag
  }

trait Android[F[_, _], C]
extends UiContext[F[?, C]]
{
  def loadFragment(fragment: FragmentBuilder): F[String, C]

  def transitionFragment(fragment: FragmentBuilder): F[String, C]

  def showViewTree(view: View): String

  def hideKeyboard(): F[String, C]

  def startActivity(cls: Class[_ <: Activity]): F[Int, C]
}
