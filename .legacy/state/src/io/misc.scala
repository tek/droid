package tryp
package droid
package state
package io

import shapeless.tag.@@

import iota._

import tryp.state.From

object misc
extends MiscCombinators

trait MiscCombinators
extends ViewCombinators
{
  def click(msg: Message)(implicit mcomm: MComm) =
    k(_.onClick(mcomm.unsafeSend(msg)))
}
