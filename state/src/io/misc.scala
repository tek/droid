package tryp
package droid
package state
package io

import fs2.Strategy

import shapeless.tag.@@

import iota._

import tryp.state.From

object misc
extends MiscCombinators

trait MiscCombinators
extends ViewCombinators
{
  def click(msg: Message)
  (implicit sender: Machine @@ From, strat: Strategy, comm: Comm) =
    k(_.onClick(comm.send(msg)))
}
