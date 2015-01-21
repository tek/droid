package tryp.droid

class Messages
{
  case class Debug()
  case class Filter(query: String)
  case class ToolbarTitle(title: String)
  case class ToolbarView(view: Fragment)
  case class Add()
  case class Navigation(target: NavigationTarget)
  case class Inject(name: String, value: Any)
  case class Back()
  case class ShowDetails(data: Any)
}

object Messages extends Messages
