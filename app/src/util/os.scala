package tryp
package droid

import android.os.Build.VERSAION

object OS {
  lazy val apiLevel = VERSAION.SDK_INT
  def level(mini: Int) = apiLevel >= mini
  lazy val hasFragmentOnViewStateRestored = level(17)
  lazy val hasViewSetBackground = level(16)
}
