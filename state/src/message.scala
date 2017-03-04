package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity
import android.app.Application

import iota.ViewTree

case class SetActivity(activity: Activity)
extends Message

case class StartActivity(agent: ActivityAgent)
extends Message

case class SetAgent(agent: ActivityAgent)
extends Message

case object AgentInitialized
extends Message

case class ActivityAgentStarted(agent: ActivityAgent)
extends Message

case object CreateContentView
extends Message

case class SetContentView(view: View, sender: Option[Machine])
extends Message

case class SetContentTree(tree: ViewTree[_ <: ViewGroup],
  sender: Option[Machine])
extends Message

case class ContentViewReady(agent: ActivityAgent)
extends Message

case object Ready
extends MState

case class ASData(app: Application, activity: Option[Activity],
  agent: Option[ActivityAgent])
extends MState

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
