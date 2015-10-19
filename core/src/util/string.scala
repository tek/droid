package tryp.util

object Strings {
  def camel2mixed(culprit: String) = {
    culprit.substring(0, 1).toLowerCase + culprit.substring(1, culprit.length)
  }

  def objectName(obj: Any) = {
    val name = obj.getClass.getSimpleName
    name.split("\\$") lift 0 getOrElse name
  }

  def objectName2mixed(obj: Any) = {
    camel2mixed(objectName(obj))
  }
}
