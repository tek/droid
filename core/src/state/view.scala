package tryp
package droid

import concurrent.duration._

import scalaz._, Scalaz._
import concurrent.Task

import State._

trait ViewStateImpl
extends DroidStateEC
{
  val transitions: ViewTransitions = {
    case UiTask(ui, timeout) ⇒ {
      case s ⇒
        s << Task(scala.concurrent.Await.result(ui.run, timeout))
    }
  }
}
