package tryp
package droid

import android.os.Build.VERSION

object OS {
  lazy val apiLevel = VERSION.SDK_INT
  def level(mini: Int) = apiLevel >= mini
}
