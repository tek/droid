package tryp.droid.util.bundle

import scala.language.postfixOps
import scala.collection.JavaConversions._

import android.os.Bundle

import tryp.droid.util.Params

object AndroidExt {
  implicit class BundleExt(bundle: Bundle) {
    def toParams: Params = {
      val params: Map[String, String] = bundle.keySet map { key =>
      key -> bundle.getString(key)
      } toMap
      val prm = new Params(params)
      prm
    }
  }
}
