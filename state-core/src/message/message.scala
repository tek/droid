package tryp
package droid
package state
package core

import ZS._

trait Message
extends Logging
{
  def internal = Internal(this)
  def toAgent = ToAgent(this)
  def toLocal = ToLocal(this)
  def toSub = ToSub(this)
  def toRoot = ToRoot(this)
  def publish = toRoot
}

trait InternalMessage
extends Message

trait Loggable
extends Message
{
  def message: String
}

case class UnknownResult[A: Show](result: A)
extends Loggable
{
  def message = result.show.toString
}

trait MessageInstances
{
  implicit def messageShow[A <: Message] = Show.shows[A] {
    case res: Loggable => s"${res.className}(${res.message})"
    case a => a.toString
  }

  type MProc = Process[Task, Message]

  implicit lazy val mProcMonoid =
    Monoid.instance[MProc]((a, b) => a.merge(b), Process.halt)

  implicit lazy val mProcMonoidNon: algebra.Monoid[MProc] =
    new algebra.Monoid[MProc] {
      def empty = Process.halt
      def combine(x: MProc, y: MProc) = x.merge(y)
    }
}

object Message
extends MessageInstances
