package tryp.droid

class Messages
{
  case class Debug()
  case class Filter(query: String)
}

object Messages extends Messages
