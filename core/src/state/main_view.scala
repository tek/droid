package tryp
package droid

import macroid.FullDsl._

import ViewState._

import scalaz._, Scalaz._

object MainViewMessages
{
  case class LoadUi(view: Ui[View])
  extends Message

  case class LoadContent(view: View)
  extends Message

  case class LoadFragment(fragment: () ⇒ Fragment)
  extends Message

  case class ContentLoaded(view: Ui[View])
  extends Message

  case object Back
  extends Message
}
import MainViewMessages._

trait MainViewImpl
extends DroidState
{
  override def description = "main view state"

  val transitions: ViewTransitions = {
    case LoadFragment(fragment) ⇒ loadFragment(fragment)
    case ContentLoaded(view) ⇒ contentLoaded(view)
    case Back ⇒ back
  }

  def loadFragment(fragment: () ⇒ Fragment): ViewTransition = {
    case s ⇒
      s << ctx.transitionFragment(FragmentBuilder(fragment, Id.content))
  }

  def contentLoaded(view: Ui[View]): ViewTransition = {
    case s ⇒ s <<
      LogInfo(s"Loaded content view:\n${ctx.showViewTree(view.get)}")
  }

  def back: ViewTransition = {
    case s ⇒
      s << Ui { nativeBack() }
  }

  def nativeBack(): Unit = ()
}
