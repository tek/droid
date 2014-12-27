package tryp.droid

class Messages
{
  case class Debug()
  case class Filter(query: String)
  case class DrawerClick(position: Int)
}

object Messages extends Messages
