package tryp.unit

import tryp.slick.TestFileDbInfo

trait UnitTestApplication
{ self: tryp.Application ⇒

  override def setupEnv() = {
    tryp.setEnv(tryp.meta.UnitTestEnv)
  }

  override def setupDbInfo(name: String) = {
    DbMeta.setDbInfo(TestFileDbInfo(name))
  }
}
