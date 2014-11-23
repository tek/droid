package tryp.droid.res

import android.content.Context

case class Resources(implicit val context: Context)
extends tryp.droid.Basic
with tryp.droid.Preferences
{
}
