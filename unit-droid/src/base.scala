package tryp
package droid
package unit

import reflect.macros.blackbox

import android.widget._
import android.support.v7.widget.RecyclerView

import akka.actor.{Props, ActorLogging, ActorRef}

import scalaz._, Scalaz._

import shapeless._

import droid.state._
import view._

import tryp.meta.LensOps._

class CoreActor
extends TrypActivityActor[SpecActivity]
with ActorLogging
{
  def receive = receiveUi andThen {
    case a ⇒
      receiveBasic(a)
  }
}

trait SpecViewMachine
extends SimpleViewMachine
{
  import TextCombinators._
  import iota._

  import android.view.ViewGroup.LayoutParams._

  lazy val search = w[AutoCompleteTextView] >>= large[AutoCompleteTextView]

  lazy val layoutIO = {
    l[FrameLayout](
      (l[RelativeLayout](search :: HNil) >>=
        lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT)
        ) :: HNil
      )
    }
}

case class SpecFragment()
extends VSTrypFragment
{
  lazy val viewMachine: SpecViewMachine = new SpecViewMachine {}
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

class ThinSpecActivity
extends Activity
with ViewActivity
{
  override def changeTheme(theme: String, restart: Boolean = true) { }

  override lazy val viewMachine =
    new RecyclerSpecMachine {
      lazy val adapter = new StringRecyclerAdapter {}
    }

  // lazy val navigation = {
  //   Navigation.simple(ActivityNavigationTarget("test", main))
  // }
}

class SpecActivity1
extends SpecActivity
{
  override def frag = SpecFragment.apply
}

abstract class ActivitySpec[A <: Activity]
extends UnitSpecs2Spec[A]
with Matchers
with HasActivity

abstract class SpecBase
extends ActivitySpec[SpecActivity1]
{
  def activityClass = classOf[SpecActivity1]
}

class FragSpecAnn(val c: blackbox.Context)
extends SimpleAnnotation
{
  import c.universe._

  def apply(annottees: Annottees) = {
    val frag = params.headOption.getOrAbort("no fragment class specified")
    val act = TypeName(frag.toString).suffix("Activity")
    val compName = annottees.comp.term
    val actCls = List[Tree](
      q"""
      class $act
      extends SpecActivity
      {
        override def frag = $frag.apply
      }
      """
      )
    val actCtor = List[Tree](
      q"def activityClass = classOf[$compName.$act]"
    )
    val bases = List(tq"ActivitySpec[$compName.$act]", tq"DefaultStrategy")
    val l1 = (Annottees.cls ^|-> ClassData.bases).append(bases)
    val l2 = (Annottees.cls ^|-> ClassData.body ^|-> BodyData.misc)
      .append(actCtor)
    val l3 = (Annottees.comp ^|-> ModuleData.body ^|-> BodyData.misc)
      .append(actCls)
    l3(l2(l1(annottees)))
  }
}

@anno(FragSpecAnn) class frag

class DummyAndroidUiContext
extends AndroidUiContext
{
  def loadFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def transitionFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def showViewTree(view: View) = "not implemented"

  def notify(id: String): Ui[Any] = Ui("asdf")

  def hideKeyboard(): Ui[String] = Ui("asdf")
}
