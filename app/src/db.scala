package tryp
package droid

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
extends Logging
{
  implicit def dbInfo = DbMeta.dbInfo

  implicit val dbTimeout = Timeout(5 seconds)

  implicit def ec: EC

  lazy val api = dbInfo.profile.api
}

trait DbProfile
extends DbAccess
{
  implicit def profile = dbInfo.profile
}
