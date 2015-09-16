package tryp.droid.meta

import macroid._

import tryp.slick.DbInfo

class ActionMacroidOps[A](a: AnyAction[A])
{
  def ui(implicit dbInfo: DbInfo, ec: EC) = {
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
