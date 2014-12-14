package tryp.droid.util

trait MetadataExt
{
  implicit class `class name shortcut`(a: AnyRef) {
    def className = a.getClass.getSimpleName
  }
}
