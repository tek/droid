package tryp
package droid
package test

import android.widget._

import akka.actor.{Props, ActorLogging, ActorRef}

class CoreActor
extends TrypActivityActor[SpecActivity]
with ActorLogging
{
  def receive = receiveUi andThen {
    case a ⇒
      receiveBasic(a)
  }
}

trait SpecViewState
extends ViewState[FrameLayout]
{
  import iota._

  import android.view.ViewGroup.LayoutParams._

  lazy val searchB = w[AutoCompleteTextView] >>= large

  def search = searchB.v

  def layoutIOT =
    l[FrameLayout](
      l[RelativeLayout](
        searchB
      ) >>= lp(MATCH_PARENT, MATCH_PARENT)
    )
}

case class SpecFragment()
extends VSTrypFragment
{
  lazy val viewState = new SpecViewState {}
}

class SpecActivity
extends Activity
with TrypTestActivity
{
  def actorsProps = Nil

  val coreActorProps = Props(new CoreActor)

  lazy val navigation = {
    Navigation.simple(NavigationTarget("test", () ⇒ SpecFragment()))
  }
}

abstract class SpecBase
extends TrypUnitSpecs2Spec[SpecActivity]
with tryp.Matchers
with HasActivity
{
  def activityClass = classOf[SpecActivity]
}
