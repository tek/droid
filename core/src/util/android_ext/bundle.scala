package tryp.droid.meta

import android.os.Bundle

import scala.language.postfixOps
import scala.collection.JavaConversions._

import tryp.Params

final class BundleOps(bundle: Bundle) {
  def toParams = Params(toMap)

  def toMap = {
    bundle.keySet
      .map { key ⇒ key → Option(bundle.getString(key)) }
      .collect { case (key, Some(value)) ⇒ key → value }
      .toMap
  }
}

trait ToBundleOps
{
  implicit def ToBundleOps(bundle: Bundle) = new BundleOps(bundle)
}
