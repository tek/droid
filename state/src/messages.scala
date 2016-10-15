package tryp
package droid
package state

case class Create(args: Params, state: Option[Bundle])
extends Message

case object Resume
extends Message

case object Update
extends Message

case class Toast(id: String)
extends Message
