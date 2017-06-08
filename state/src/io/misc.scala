package tryp
package droid
package state
package io

import iota._

object misc
extends MiscCombinators

trait MiscCombinators
extends ViewCombinators
{
  def click(comm: Comm, msg: Message) =
    k(_.onClick(comm.send(msg)))
}
