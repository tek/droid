package tryp
package droid
package state
package core

import shapeless.syntax.std.tuple._

import scalaz._, Scalaz._
import stream.process1
import Process._

trait BasicState
case object Pristine extends BasicState
case object Initialized extends BasicState
case object Initializing extends BasicState

trait Data
case object NoData extends Data

case class Zthulhu(state: BasicState = Pristine, data: Data = NoData)

// create a state machine from a source process emitting Message instances,
// a transition function that turns messages and states into new states and
// side effects which optionally produce new messages, and a handler process
// that extract those new messages from the side effects.
// returns a writer that emits the states on the output side and the new
// messages on the write side.
final class ZthulhuCtor(source: MProc)
(implicit log: Logger)
{
  import Zthulhu._

  def fsm[Z](initial: Z, transition: (Z, Message) => Maybe[(Z, Effect)]) = {
    source
      .pipe(transLoop(initial)(transition)(fatalTransition))
      .flatMapO(handleResult)
  }
}

trait ToZthulhuCtor
{
  implicit def ToZthulhuCtor(source: MProc)(implicit log: Logger) = 
    new ZthulhuCtor(source)
}

object Zthulhu
extends BoundedCachedPool
{
  def name = "zthulhu"

  override def maxThreads = 3

  /* @param z current state machine
   * @param f (Z, M) => (Z, E), transforms a state and message into a new state
   * and a Process of effects of type E
   *
   * first, receive one item of type M representing a Message
   * calculate new state, may be empty, then the old state is used
   * the new state is emitted on the output side, the effects returned from
   * the transition function, if any, are emitted on the writer side and the
   * current state z is replaced by the return value of the transition function
   * in any case, the new or old state is used to recurse
   * error callbacks are invoked if the transition or effect throws an
   * exception and their results emitted
   */
  def transLoop[Z, E, M]
  (z: Z)
  (f: (Z, M) => Maybe[(Z, E)])
  (transError: (Z, M, Throwable) => E)
  : Writer1[Z, M, E] = {
    receive1 { (a: M) =>
      val (state, effect) =
        Task(f(z, a)).runDefault match {
          case \/-(Just((nz, e))) => nz.just -> e.just
          case \/-(Empty()) => Maybe.empty[Z] -> Maybe.empty[E]
          case -\/(t) => Maybe.empty[Z] -> transError(z, a, t).just
        }
      val w = state.cata(emitW, halt)
      val o = effect.cata(emitO, halt)
      val l = transLoop(state | z)(f)(transError)
      w ++ o ++ l
    }
  }

  def fatalTransition[Z](state: Z, cause: Message, t: Throwable) = {
    emit(LogFatal(s"transitioning $state for $cause", t).publish.fail)
  }

  // pipe the result computed by the (possibly effectful) processes produced
  // by the state transition, then extract messages from the result.
  // the effectful nature necessitates catching exceptions via the attempt()
  // operation, which returns a writer with Throwables on the W side.
  // those are transformed to Result instances, then joined with the
  // successful results, also on the W side.
  // the results are then disassembled into single Message instances.
  def handleResult(effect: Effect)(implicit log: Logger) = {
    effect
      .attempt(t => emit(LogFatal("performing effect", t).publish.fail))
      .mergeO
      .mapO {
        case scalaz.Success(trans) =>
          log.debug(s"task succeeded with ${trans.show}")
          List(trans)
        case scalaz.Failure(transs) =>
          log.error(s"task failed with ${transs.show}")
          transs.toList
      }
      .pipeO(process1.unchunk)
  }
}
