package tryp
package droid
package core
package syntax
package std

import scala.language.postfixOps
import scala.collection.JavaConversions._

final class BundleOps(bundle: Bundle) {
  def toMap = {
    bundle.keySet
      .map { key => key -> Option(bundle.getString(key)) }
      .collect { case (key, Some(value)) => key -> value }
      .toMap
  }
}

trait ToBundleOps
{
  implicit def ToBundleOps(bundle: Bundle) = new BundleOps(bundle)
}
