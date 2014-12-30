package tryp.droid.util

trait StringOpsExt
{
  implicit class `String extensions`(name: String) {
    def snakeCase = {
      name.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
        .replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase
    }
  }
}
