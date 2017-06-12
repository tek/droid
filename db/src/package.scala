package tryp
package droid
package db

import scalaz.ValidationNel

trait Names
{
  import _root_.slick.dbio.Effect
  type ActionResult[E, A] = ValidationNel[E, A]
  type ValidationAction[E, A] = SlickAction[ActionResult[E, A], Effect.All]
  type Action[A] = ValidationAction[String, A]

  type SlickEffect = _root_.slick.dbio.Effect
  type SlickAction[A, E <: SlickEffect] =
    _root_.slick.dbio.DBAIOAction[A, _root_.slick.dbio.NoStream, E]
}

@integrate(droid.core, droid.state, tryp.slick, tryp.state, tryp.app)
object `package`
extends Names
with DbStateEffectInstances
