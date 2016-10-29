package tryp
package droid
package integration

import shapeless._

class AppSpec
extends TrypIntegrationSpec(classOf[IntStateActivity])
{
  def testSomething() = {
    Thread.sleep(5000)
    p("--------------")
  }
}
