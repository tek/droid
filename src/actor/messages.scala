package tryp.droid

import android.transitions.everywhere.TransitionSet

class Messages
{
  case class Filter(query: String)
  case class ToolbarTitle(title: String)
  case class ToolbarView(view: Fragment)
  case class Add()
  case class Navigation(target: NavigationTarget)
  case class Inject(name: String, value: Any)
  case class Back(result: Option[Any] = None)
  case class Result(data: Any)
  case class ShowDetails(data: Model)
  case class Log(message: String)
  case class Transitions(transitions: Seq[TransitionSet])
  case class Showcase()
  case class DataLoaded()
  case class Scrolled(view: ViewGroup, dy: Int)
  case object Update
}

object Messages extends Messages
