package tryp
package droid

import state._

object NavMessages
{
  case object Home
  extends Message

  case class Index(index: Int)
  extends Message

  case class Target(target: NavigationTarget)
  extends Message

  case class LoadTarget(target: NavigationTarget)
  extends Message

  case class SetNav(nav: Navigation)
  extends Message

  case object Back
  extends Message
}
import NavMessages._

trait NavMachine
extends SimpleDroidState
{
  override def description = "nav state"

  case class NavData(navigation: Navigation, history: List[NavigationTarget])
  extends Data
  {
    def current = history.headOption

    def isCurrent = current.contains _
  }

  val transitions: ViewTransitions = {
    case SetNav(n: Navigation) ⇒ setNav(n)
    case Create(_, _) ⇒ resumeNav
    case Target(t) ⇒ target(t)
    case Index(i) ⇒ index(i)
    case Home ⇒ index(0)
    case Back ⇒ back
    case LoadTarget(t) ⇒ loadTarget(t)
  }

  def setNav(nav: Navigation): ViewTransition = {
    case S(s, NoData) ⇒ S(Initialized, NavData(nav, Nil))
    case S(s, NavData(_, history)) ⇒ S(Initialized, NavData(nav, history))
  }

  def resumeNav: ViewTransition = {
    case s @ S(_, n: NavData) ⇒
      val initial = n.current some[Message](Target(_)) none(Home)
      s << initial
  }

  def target(t: NavigationTarget): ViewTransition = {
    case S(s, n @ NavData(nav, hist)) if !n.isCurrent(t) ⇒
        val newHist = t :: hist.filterNot(_ == t)
        S(s, NavData(nav, newHist)) << LoadTarget(t)
  }

  def index(i: Int): ViewTransition = {
    case s @ S(_, NavData(nav, _)) ⇒
      s << nav(i).map(Target(_))
  }

  def back: ViewTransition = {
    case S(s, NavData(nav, cur :: next :: hist)) ⇒
      S(s, NavData(nav, next :: hist)) << LoadTarget(next)
    case s ⇒ s << MainViewMessages.Back
  }

  def loadTarget(t: NavigationTarget): ViewTransition = {
    case s ⇒ s << MainViewMessages.LoadFragment(t.fragment, t.title)
  }
}
