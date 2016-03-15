package tryp.droid

import scala.language.postfixOps
import scala.collection.JavaConversions._

trait BundleExt
{
  implicit class BundleOps(bundle: Bundle) {
    def toMap = {
      bundle.keySet
        .map { key => key -> Option(bundle.getString(key)) }
        .collect { case (key, Some(value)) => key -> value }
        .toMap
    }
  }
}
