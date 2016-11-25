package tryp
package droid
package db

import java.io.File

import slick._

object Db
{
  implicit def fromContext(implicit c: Context): DbInfo = {
    val dbPath = new File(c.getFilesDir, "tryp.db")
    DroidDbInfo(dbPath.toString)
  }

  implicit def profileFromDbInfo(implicit dbi: DbInfo) = dbi.profile
}

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

  // implicit val dbTimeout = Timeout(5 seconds)

  implicit def ec: EC

  lazy val api = dbInfo.profile.api
}

trait DbProfile
extends DbAccess
{
  implicit def profile = dbInfo.profile
}
