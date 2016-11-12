package tryp
package droid
package state
package io

import shapeless.tag.@@

import tryp.state.From

object misc
extends MiscCombinators

trait MiscCombinators
extends ViewCombinators
{
  def click(msg: Message)(implicit sender: Machine @@ From) =
    k { (v: View) =>
      v.clickListen(_ => sender.send(msg))
    }
}
