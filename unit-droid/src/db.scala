package tryp
package droid
package unit

import tryp.slick.TestFileDbInfo

case class DbName(name: String)
extends AnyVal

object Db
{
  implicit def fromDbName(implicit n: DbName): DbInfo = {
    TestFileDbInfo(n.name)
  }

  implicit def profileFromDbInfo(implicit dbi: DbInfo) = dbi.profile
}
