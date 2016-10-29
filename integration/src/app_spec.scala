package tryp
package droid
package integration

import android.test.ActivityInstrumentationTestCase2

class AppSpec
extends ActivityInstrumentationTestCase2(classOf[android.app.Activity])
{
  def p[A](s: A) = {
    Log.e("droid", s.toString)
  }

  def testSomething() = {
    p("==================================")
    try {
      p("--------------")
      p(cats.Functor)
      p(shapeless.HList)
      p("--------------")
    } catch {
      case e: Throwable => p(e)
    }
  }
}
