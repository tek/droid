package tryp
package droid
package unit

import android.widget._
import android.support.v7.widget.RecyclerView

import akka.actor.{Props, ActorLogging, ActorRef}

import scalaz._, Scalaz._

import droid.state._
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

class SpecActivity
extends Activity
with UnitActivity
{
  def actorsProps = Nil

  val coreActorProps = Props(new CoreActor)

  lazy val navigation = {
    Navigation.simple(NavigationTarget("test", frag))
  }

  def frag: () ⇒ Fragment = ???
}

abstract class TestViewActivity
extends Activity
with ViewActivity
{
  override def changeTheme(theme: String, restart: Boolean = true) { }
}

abstract class ActivitySpec[A <: Activity]
extends UnitSpecs2Spec[A]
with Matchers
with HasActivity

class DummyAndroidUiContext
extends AndroidUiContext
{
  def loadFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def transitionFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def showViewTree(view: View) = "not implemented"

  def notify(id: String): Ui[Any] = Ui("asdf")

  def hideKeyboard(): Ui[String] = Ui("asdf")
}
