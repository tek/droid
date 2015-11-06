package tryp
package droid

import akka.actor._
import akka.util._
import akka.pattern.ask

trait Agent[M]
{
  def !(m: M)

  def ?(m: M)
  (implicit timeout: Timeout, sender: ActorRef = Actor.noSender): Future[_]
}

case class AkkaAgent(actor: ActorSelection)
extends Agent[Any]
{
  def !(m: Any) = actor ! m

  def ?(m: Any)(implicit timeout: Timeout, sender: ActorRef = Actor.noSender) =
    actor ? m
}

case class DummyAgent()
extends Agent[Any]
{
  def !(m: Any) = ()
  def ?(m: Any)(implicit timeout: Timeout, sender: ActorRef = Actor.noSender) =
    Future.successful(())
}

trait Communicator
{
  def core: Agent[Any]
}

object Communicator
{
  implicit def akkativityCommunicator(implicit a: Akkativity) =
    AkkativityCommunicator(a)
}

case class AkkativityCommunicator(akk: Akkativity)
extends Communicator
{
  def core = AkkaAgent(akk.core)
}

case class DummyCommunicator()
extends Communicator
{
  def core = DummyAgent()
}

trait HasComm
{
  implicit def comm: Communicator
}
