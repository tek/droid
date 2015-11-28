package tryp
package droid
package unit

import android.widget._

import akka.actor.{Props, ActorLogging, ActorRef}

import state._
import view._

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

  lazy val search = w[AutoCompleteTextView] >>= large

  def layoutIOT =
    l[FrameLayout](
      l[RelativeLayout](
        search
      ) >>= lp(MATCH_PARENT, MATCH_PARENT)
    )
}

case class SpecFragment()
extends VSTrypFragment
{
  lazy val viewState: SpecViewState = new SpecViewState {}
}

class SpecActivity
extends Activity
with UnitActivity
{
  def actorsProps = Nil

  val coreActorProps = Props(new CoreActor)

  lazy val navigation = {
    Navigation.simple(NavigationTarget("test", () ⇒ SpecFragment()))
  }
}

abstract class SpecBase
extends UnitSpecs2Spec[SpecActivity]
with Matchers
with HasActivity
{
  def activityClass = classOf[SpecActivity]
}


class DummyAndroidUiContext
extends AndroidUiContext
{
  def loadFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def transitionFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def showViewTree(view: View) = "not implemented"

  def notify(id: String): Ui[Any] = Ui("asdf")
}
