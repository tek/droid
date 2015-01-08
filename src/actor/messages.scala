package tryp.droid

class Messages
{
  case class Debug()
  case class Filter(query: String)
  case class ToolbarTitle(title: String)
  case class ToolbarView(view: Fragment)
  case class Add()
  case class Navigation(target: NavigationTarget)
}

object Messages extends Messages
