package tryp.droid

class Messages
{
  case class Debug()
  case class Filter(query: String)
  case class DrawerClick(position: Int)
  case class ToolbarTitle(title: String)
  case class ToolbarView(view: Fragment)
  case class Add()
}

object Messages extends Messages
