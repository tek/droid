package tryp.droid

class Messages
{
  case class Debug()
  case class Filter(query: String)
  case class DrawerClick(position: Int)
  case class ToolbarTitle(title: String)
}

object Messages extends Messages
