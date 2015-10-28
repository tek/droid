package tryp
package droid
package unit

import slick.TestFileDbInfo

trait UnitTestApplication
{ self: Application ⇒

  override def setupEnv() = {
    setEnv(tryp.meta.UnitTestEnv)
  }

  override def setupDbInfo(name: String) = {
    DbMeta.setDbInfo(TestFileDbInfo(name))
  }
}