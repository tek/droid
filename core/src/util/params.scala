package tryp.droid

case class Params(params: Map[String, String] = Map())
{
  def apply(key: String) = params.apply(key)
}

object Params
{
  implicit def mapToParams(params: Map[String, String]) = {
    Params(params)
  }

  def fromErasedMap(m: Map[_, _]) = {
    val params = m filter { case (key, value) ⇒
      (key, value) match {
        case (a: String, b: String) ⇒ true
        case _ ⇒ false
      }
    } map { case (key, value) ⇒
      (key, value) match {
        case (a: String, b: String) ⇒ (a, b)
      }
    }
    Params(params)
  }
}
