package tryp

import concurrent.duration._

import akka.util.Timeout

object DbMeta
{
  var dbInfoInst: Option[DbInfo] = None
  def dbInfo = {
    dbInfoInst getOrElse(sys.error("db accessed before initialization"))
  }
  def setDbInfo(info: DbInfo) = {
    dbInfoInst = Some(info)
  }
}

trait DbAccess
extends AkkaComponent
{
  implicit def dbInfo = DbMeta.dbInfo

  implicit val timeout = Timeout(5 seconds)
}

trait DbProfile
extends DbAccess
{
  implicit def profile = dbInfo.profile
}
