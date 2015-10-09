package tryp.droid

import scala.language.postfixOps
import scala.collection.JavaConversions._

import tryp.droid.util.Params

trait BundleExt
{
  implicit class BundleOps(bundle: Bundle) {
    def toParams = Params(toMap)

    def toMap = {
      bundle.keySet
        .map { key ⇒ key → Option(bundle.getString(key)) }
        .collect { case (key, Some(value)) ⇒ key → value }
        .toMap
    }
  }
}
