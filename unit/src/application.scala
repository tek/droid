package tryp.droid
package unit

import slick.TestFileDbInfo

trait UnitTestApplication
{ self: Application ⇒

  override def setupEnv() = {
    setEnv(meta.UnitTestEnv)
  }

  override def setupDbInfo(name: String) = {
    DbMeta.setDbInfo(TestFileDbInfo(name))
  }
}
