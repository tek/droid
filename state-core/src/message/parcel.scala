package tryp
package droid
package state
package core

import ZS._

sealed trait Parcel
extends InternalMessage
{
  def message: Message

  def fail = this.failureNel[Parcel]

  def success = this.successNel[Parcel]
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
  implicit val ShowParcel = Show.shows[Parcel](_.message.shows)
}

object Parcel
extends ParcelInstances
{
  implicit def messageToParcel[A <: Message](m: A)
  (implicit pf: PublishFilter[A]): Parcel = {
    pf.parcel(m)
  }
}
