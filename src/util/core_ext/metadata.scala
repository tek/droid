package tryp.droid.util

import scala.reflect.ClassTag

trait MetadataExt
{
  implicit class `class name shortcut`(a: AnyRef) {
    def className = a.getClass.getSimpleName
  }

  implicit class `class name shortcut for ClassTag`(a: ClassTag[_]) {
    def className = a.runtimeClass.getSimpleName
  }

  implicit class `class name shortcut for Class`(a: Class[_]) {
    def className = a.getSimpleName
  }
}
