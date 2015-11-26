package tryp
package droid
package unit

import android.widget._

import akka.actor.{Props, ActorLogging, ActorRef}

import state._

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
extends TrypFragment
with view.ExtViews
{
  // lazy val viewState = new SpecViewState {}

  def layout(state: Bundle) = {
    import android.view.ViewGroup.LayoutParams._
    Ui{
      l[FrameLayout](
        l[RelativeLayout](
          w[AutoCompleteTextView]
        ) >>= iota.lp(MATCH_PARENT, MATCH_PARENT)
      ).perform()
    }
  }
}

class SpecActivity
extends Activity
with test.TrypTestActivity
{
  def actorsProps = Nil

  val coreActorProps = Props(new CoreActor)

  lazy val navigation = {
    Navigation.simple(NavigationTarget("test", () ⇒ SpecFragment()))
  }
}

abstract class SpecBase
extends test.TrypUnitSpecs2Spec[SpecActivity]
with tryp.Matchers
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
