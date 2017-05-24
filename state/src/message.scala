package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity
import android.app.Application

import iota.ViewTree

case class SetActivity(activity: Activity)
extends Message

case object AgentInitialized
extends Message

case object CreateContentView
extends Message

case class SetContentView(view: View)
extends Message

case class SetContentTree(tree: ViewTree[_ <: ViewGroup])
extends Message

case object Ready
extends CState

trait ActivityLifecycleMessage
extends Message
{
  def activity: Activity
}

case class OnStart(activity: Activity)
extends ActivityLifecycleMessage

case class OnResume(activity: Activity)
extends ActivityLifecycleMessage

case class ToAppState(message: Message)
extends Message

case object InitApp
extends Message

case class SetApplication(app: Application)
extends Message
