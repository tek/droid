package tryp
package droid
package integration

abstract class IntStateSpec
extends StateSpec[IntStateActivity](classOf[IntStateActivity])
{ self =>
  override def stateApp = super.stateApp match {
    case a: IntApplication => a
    case a => sys.error(s"application is not an IntApplication: $a")
  }

  def initialUi: Option[ViewAgent] = None
}

case class SimpleAg(override val initialUi: Option[ViewAgent])
extends Simple

class SimpleIntStateSpec
extends IntStateSpec
{
  def agent: ActivityAgent = SimpleAg(initialUi)
}

case class ExtAg(override val initialUi: Option[ViewAgent])
extends Ext

class ExtIntStateSpec
extends IntStateSpec
{
  def agent: ActivityAgent = ExtAg(initialUi)
}
