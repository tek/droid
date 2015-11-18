package tryp
package droid

import scalaz._, Scalaz._

import macroid._

import tryp.UiContext

case class FragmentBuilder(ctor: () ⇒ Fragment, id: Id,
  tagO: Option[String] = None)
  {
    def apply() = ctor()

    def tag = tagO | id.tag
  }

class ActionMacroidOps[A](a: AnyAction[A])
{
  def uiBlocking(implicit dbInfo: DbInfo, ec: EC) = {
    Ui.nop.flatMap { _ ⇒
      a.!!
      Ui.nop
    }
  }
}

trait ToActionMacroidOps
{
  implicit def ToActionMacroidOps[A](a: AnyAction[A]) = new ActionMacroidOps(a)
}

object UiActionTypes
{ type ActionResult[E, A] = ValidationNel[E, A]
  type ValidationAction[E, A] = AnyAction[ActionResult[E, A]]
  type UiActionResult[E, A] = ActionResult[E, Ui[A]]
  type UiAction[E, A] = ValidationAction[E, Ui[A]]
  type Action[A] = ValidationAction[String, A]
}
import UiActionTypes._

trait AndroidUiContext
extends UiContext[Ui]
{
  def loadFragment(fragment: FragmentBuilder): Ui[String]

  def transitionFragment(fragment: FragmentBuilder): Ui[String]

  def showViewTree(view: View): String
}

trait AndroidContextUiContext
extends AndroidUiContext
with HasContext
with ResourcesAccess
with Logging
{
  def aContext = context

  def showViewTree(view: View): String = {
    view.viewTree.drawTree
  }

  override def notify(id: String): Ui[Any] = Ui(log.info(id))

  def loadFragment(fragment: FragmentBuilder) = {
    Ui("AndroidContextUiContext cannot handle fragments")
  }

  def transitionFragment(fragment: FragmentBuilder) = {
    loadFragment(fragment)
  }
}

trait AndroidHasActivityUiContext
extends AndroidContextUiContext
with ResourcesAccess
with Snackbars
with FragmentManagement
with Transitions
with TrypActivityAccess
with HasSettings
{
  override def loadFragment(fragment: FragmentBuilder) = {
    Ui {
      fragment() tap { inst ⇒
        replaceFragment(fragment.id, inst, false, fragment.tag, false)
      }
      "fragment loaded successfully"
    }
  }

  override def transitionFragment(fragment: FragmentBuilder) = {
    settings.app.bool("view_transitions", true)().fold(trypActivity, None)
      .some { a ⇒
        Ui[String] {
          implicit val handler: FragmentManagement = a
          val ui = Macroid.frag(fragment(), fragment.id, fragment.tag)
          a.transition(ui)
          "Transition successful"
        }
      }
      .none(loadFragment(fragment) map(_ ⇒ "Cannot transition fragment"))
  }

  override def failure[E: Show](e: NonEmptyList[E]) = {
    Log.d(s"handling failure in activity: $e")
    snackbarLiteral(e.map(_.show).toList.mkString("\n"))
  }

  // TODO split in notifyLiteral and notifyRes or similar
  override def notify(id: String) = mkToast(id)
}

trait AndroidActivityUiContext
extends AndroidHasActivityUiContext
{
  def getFragmentManager = activity.getFragmentManager

  def view = activity.view
}

class DefaultAndroidActivityUiContext(implicit val activity: Activity)
extends AndroidActivityUiContext

object AndroidActivityUiContext
{
  def default(implicit a: Activity) = new DefaultAndroidActivityUiContext
}

trait AndroidFragmentUiContext
extends AndroidHasActivityUiContext
{
  val fragment: Fragment

  def getFragmentManager = fragment.getChildFragmentManager

  def view = activity.view
}

class DefaultAndroidFragmentUiContext(implicit val fragment: Fragment)
extends AndroidFragmentUiContext
{
  val activity = fragment.activity
}

object AndroidFragmentUiContext
{
  def default(implicit f: Fragment) = new DefaultAndroidFragmentUiContext
}

class UiOps[A](ui: Ui[A])
(implicit ctx: AndroidUiContext, ec: EC)
{
  def attemptUi = {
    Log.d(s"running the Ui")
    Ui.run(ui)
      .flatMap(handleUiResult)
      .andThen { case Failure(e) ⇒ uiError(e) }
  }

  def handleUiResult(a: A) = {
    Log.d(s"handling Ui result $a")
    Future.successful(a)
  }

  def uiError(e: Throwable) = {
    Log.d(s"logging Ui error")
    ctx.uiError(e)
  }
}

trait ToUiOps
{
  implicit def ToUiOps[A](a: Ui[A])
  (implicit ctx: AndroidUiContext, ec: EC, info: DbInfo) = {
    new UiOps(a)
  }
}

// TODO replace Log with Writer
// at the end of the universe, send written items to generic log, may be
// snackbars, stdout or android etc.
class UiValidationNelActionOps[E: Show, A](a: UiAction[E, A])
(implicit ctx: AndroidUiContext, ec: EC, info: DbInfo)
extends ToUiOps
{
  def attemptUi: Unit = {
    Log.d(s"running action")
    a.!.task runAsync {
      case \/-(r) ⇒ handleActionResult(r)
      case -\/(e) ⇒ dbError(e)
    }
  }

  def handleActionResult(result: UiActionResult[E, A]) = {
    Log.d(s"handling action result: $result")
    result fold(ctx.failure(_), _.attemptUi)
  }

  def dbError(e: Throwable) = {
    Log.d(s"logging db error")
    ctx.dbError(e)
  }
}

trait ToUiValidationNelActionOps
{
  implicit def ToUiValidationNelActionOps[E: Show, A]
  (a: UiAction[E, A])
  (implicit ec: EC, info: DbInfo, ctx: AndroidUiContext) = {
    new UiValidationNelActionOps(a)
  }
}
