package tryp.util

import android.os.Build.VERSION

object OS {
  lazy val apiLevel = VERSION.SDK_INT
  def level(mini: Int) = apiLevel >= mini
  lazy val hasFragmentOnViewStateRestored = level(17)
  lazy val hasViewSetBackground = level(16)
}
