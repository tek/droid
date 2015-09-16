package tryp.droid.unit

import tryp.slick.TestFileDbInfo

import tryp.droid._

trait UnitTestApplication
{ self: tryp.droid.Application ⇒

  override def setupEnv() = {
    tryp.setEnv(tryp.meta.UnitTestEnv)
  }

  override def setupDbInfo(name: String) = {
    DbMeta.setDbInfo(TestFileDbInfo(name))
  }
}
