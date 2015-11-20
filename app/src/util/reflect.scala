package tryp.droid

// used in loki.integration to access spinner fields
object Reflect
{
  implicit class `Any with reflectable fields`(obj: Any) {

    def field[A: ClassTag](name: String) = {
      val f = obj.getClass.getDeclaredField(name)
      f.setAccessible(true)
      f.get(obj) match {
        case a: A ⇒ Some(a)
        case _ ⇒ None
      }
    }
  }
}
