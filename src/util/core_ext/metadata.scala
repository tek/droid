package tryp.droid.util

import scala.reflect.ClassTag

trait MetadataExt
{
  private def removeDollar(s: String) = s.stripSuffix("$")

  implicit class `class name shortcut`(a: Any) {
    def className = removeDollar(a.getClass.getSimpleName)
  }

  implicit class `class name shortcut for ClassTag`(a: ClassTag[_]) {
    def className = removeDollar(a.runtimeClass.getSimpleName)
  }

  implicit class `class name shortcut for Class`(a: Class[_]) {
    def className = removeDollar(a.getSimpleName)
  }
}
