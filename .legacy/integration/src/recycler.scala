package tryp
package droid
package integration

case class RecTrans(mcomm: MComm)
extends StringRV
{
  def admit = {
    case AdapterInstalled =>
      _ << Update(List("a", "b", "c"))
  }
}

object RecMac
extends ViewMachine
{
  def transitions(mc: MComm) = RecTrans(mc)
}

object RecyclerAgent
extends ViewAgent
{
  lazy val viewMachine = RecMac
}
