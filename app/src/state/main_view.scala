package tryp
package droid

import macroid.FullDsl._

import state._
import view._

import scalaz._, Scalaz._

object MainViewMessages
{
  case class LoadUi(view: Ui[View])
  extends Message

  case class LoadContent(view: View)
  extends Message

  case class LoadFragment(fragment: () => Fragment, tag: String)
  extends Message

  case class ContentLoaded(view: Ui[View])
  extends Message

  case object Back
  extends Message
}
import MainViewMessages._

trait MainViewMachine
extends Machine
{
  import AppState._

  override def description = "main view state"

  val admit: Admission = {
    case LoadUi(ui) => loadUi(ui)
    case LoadFragment(fragment, tag) => loadFragment(fragment, tag)
    case ContentLoaded(view) => contentLoaded(view)
    case Back => back
  }

  def loadUi(ui: Ui[View]): Transit = {
    case s =>
      s
  }

  def loadFragment(fragment: () => Fragment, tag: String): Transit = {
    case s =>
      s << ContextTask(
        _.transitionFragment(FragmentBuilder(fragment, RId.content, tag.some))
          .toResult
      )
  }

  def contentLoaded(view: Ui[View]): Transit = {
    case s => s << ContextTask(ctx =>
      LogInfo(s"Loaded content view:\n${ctx.showViewTree(view.get)}")
    )
  }

  def back: Transit = {
    case s =>
      s << Ui { nativeBack() }
  }

  def nativeBack(): Unit = ()
}
