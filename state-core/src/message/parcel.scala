package tryp
package droid
package state
package core

sealed trait Parcel
extends InternalMessage
{
  def message: Message

  def fail = this.invalidNel[Parcel]

  def success = this.validNel[Parcel]
}

case class Internal(message: Message)
extends Parcel

case class ToAgent(message: Message)
extends Parcel

case class ToLocal(message: Message)
extends Parcel

case class ToRoot(message: Message)
extends Parcel

case class ToSub(message: Message)
extends Parcel

case class SpecialParcel(message: Message, agent: Boolean, local: Boolean)
extends Message

trait ParcelInstances
{
  implicit lazy val ShowParcel = cats.Show.show[Parcel](_.message.show)
}

object Parcel
extends ParcelInstances
{
  implicit def messageToParcel[A <: Message](m: A)
  (implicit pf: PublishFilter[A]): Parcel = {
    pf.parcel(m)
  }
}
