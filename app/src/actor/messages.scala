package tryp.droid

import com.google.android.gms.maps.GoogleMap

import android.transitions.everywhere.TransitionSet

class Messages
{
  case class Filter(query: String)
  case class ToolbarTitle(title: String)
  case class ToolbarView(view: Fragment)
  case object Add
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
  case class MapReady(map: GoogleMap)
  case class AuthBackend()
  case class BackendAuthFailed()
  case class Toast(id: String)
  case class StartAsyncTask(f: Future[_])
  case class CompleteAsyncTask(f: Future[_])
  case class DrawerClick(action: State.Message)
}

object Messages extends Messages
