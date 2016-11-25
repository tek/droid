package tryp
package droid
package integration

import droid.state.{AppState, MVData, ViewDataI}

class IntStateSpec[A <: StateActivity](cls: Class[A])
extends StateSpec[A](cls)
{
  def stateApp = stateActivity.stateApp match {
    case Right(a) => a match {
      case a: IntApplication => a
      case a => sys.error(s"application is not an IntApplication: $a")
    }
    case Left(a) => sys.error(a)
  }

  lazy val root = stateApp.root

  lazy val appStateMachine = root.appStateMachine

  // def activityAgent =
  //   appStateMachine.current.get.unsafePerformSync.data match {
  //     case AppState.ASData(_, Some(agent)) => agent
  //   case _ => sys.error("no activity agent running")
  //   }

  // def mainAgent =
  //   activityAgent match {
  //     case m: MainViewAgent => m
  //     case _ => sys.error("activity agent is not a main view agent")
  //   }

  // def mainUi: ViewAgent =
  //   mainAgent.mvMachine.current.get.unsafePerformSync.data match {
  //     case MVData(ui: ViewAgent) => ui
  //     case _ => sys.error("main view has no ui")
  //   }

  // def mainTree[A: ClassTag] =
  //   mainUi.viewMachine.current.get.unsafePerformSync.data match {
  //     case a: ViewDataI[_] =>
  //       a.view match {
  //       case tree: A => tree
  //       case _ => sys.error(s"view tree has wrong class: ${a.view}")
  //     }
  //     case _ => sys.error("no view tree in main ui")
  //   }
}
