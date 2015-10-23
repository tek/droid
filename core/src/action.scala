package tryp.droid

import scalaz._, Scalaz._

import macroid._

import tryp.UiContext

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
with HasContext

trait AndroidActivityUiContext
extends AndroidUiContext
with Snackbars
{
  override def failure[E: Show](e: NonEmptyList[E]) = {
    Log.d(s"handling failure in activity: $e")
    snackbarLiteral(e.map(_.show).toList.mkString("\n"))
  }

  override def notify(id: String) = mkToast(id)
}

class DefaultAndroidActivityUiContext(implicit val activity: Activity)
extends AndroidActivityUiContext

object AndroidActivityUiContext
{
  def default(implicit a: Activity) = new DefaultAndroidActivityUiContext
}

trait AndroidFragmentUiContext
extends AndroidActivityUiContext

class DefaultAndroidFragmentUiContext(implicit fragment: Fragment)
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
