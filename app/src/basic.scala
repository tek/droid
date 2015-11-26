package tryp
package droid

trait HasContext
{
  implicit def context: Context

  implicit lazy val settings = implicitly[Settings]
}

trait Basic
extends HasContext
with ResourcesAccess
with Logging
{
  type IdTypes = Int with String with Id

  def systemService[A: ClassTag](name: String) = {
    context.getSystemService(name) match {
      case a: A ⇒ a
      case _ ⇒ {
        throw new ClassCastException(
          s"Wrong class for ${implicitly[ClassTag[A]].className}!"
        )
      }
    }
  }
}

trait ActivityAccess
extends HasActivity
{
  def activitySub[A <: ActivityBase: ClassTag]: Option[A] = {
    activity match {
      case a: A ⇒ Option[A](a)
      case _ ⇒ None
    }
  }
}

trait TrypActivityAccess
extends ActivityAccess
{
  def trypActivity = activitySub[TrypActivity]
}
