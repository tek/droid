package tryp

import scala.concurrent.duration._

import akka.actor._

case object DrawerOpened
case class DrawerClosed(callback: () ⇒ Unit)
case class DrawerNavigated(callback: () ⇒ Unit)

sealed trait State
case object Open extends State
case object Navigated extends State
case object Closed extends State

class DrawerState
extends LoggingFSM[State, DrawerState.Data]
{
  import DrawerState._

  startWith(Closed, NoData)

  when(Closed) {
    case Event(DrawerOpened, _) ⇒
      goto(Open) using NoData
  }

  when(Open) {
    case Event(DrawerNavigated(callback), NoData) ⇒
      goto(Navigated) using Navigation(callback)
    case Event(DrawerClosed(callback), NoData) ⇒
      goto(Closed) using Quit(callback)
  }

  when(Navigated) {
    case Event(DrawerClosed(_), Navigation(_)) ⇒
      goto(Closed) using NoData
  }

  onTransition {
    case Navigated -> Closed ⇒
      stateData match {
        case Navigation(callback) ⇒ callback()
        case _ ⇒
      }
    case Open -> Closed ⇒
      nextStateData match {
        case Quit(callback) ⇒ callback()
        case _ ⇒
      }
  }

  whenUnhandled {
    case Event(e, s) ⇒
      log.warning(s"unhandled request ${e} in state ${stateName}/${s}")
      stay
  }

  initialize()
}

object DrawerState {
  sealed trait Data
  case object NoData extends Data
  case class Navigation(callback: () ⇒ Unit) extends Data
  case class Quit(callback: () ⇒ Unit) extends Data

  def props = Props(new DrawerState)
}
