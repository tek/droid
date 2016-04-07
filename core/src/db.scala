package tryp
package droid
package core

import java.io.File

import tryp.slick.{DbInfo, DroidDbInfo}

object Db
{
  implicit def fromContext(implicit c: Context): DbInfo = {
    val dbPath = new File(c.getFilesDir, "tryp.db")
    DroidDbInfo(dbPath.toString)
  }

  implicit def profileFromDbInfo(implicit dbi: DbInfo) = dbi.profile
}