package tryp
package droid
package state

import shapeless.syntax.std.tuple._

import scalaz._, Scalaz._
import stream.process1
import Process._

case class Zthulhu(state: BasicState = Pristine, data: Data = NoData)

object Zthulhu
extends BoundedCachedPool
{
  def name = "zthulhu"

  override def maxThreads = 3

  /* @param z current state machine
   * @param f (Z, M) ⇒ (Z, E), transforms a state and message into a new state
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
  (f: (Z, M) ⇒ Maybe[(Z, E)])
  (transError: (Z, M, Throwable) ⇒ E)
  : Writer1[Z, M, E] = {
    receive1 { (a: M) ⇒
      val (state, effect) =
        Task(f(z, a)).runDefault match {
          case \/-(Just((nz, e))) ⇒ nz.just → e.just
          case \/-(Empty()) ⇒ Maybe.empty[Z] → Maybe.empty[E]
          case -\/(t) ⇒ Maybe.empty[Z] → transError(z, a, t).just
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

  // create a state machine from a source process emitting Message instances,
  // a transition function that turns messages and states into new states and
  // side effects which optionally produce new messages, and a handler process
  // that extract those new messages from the side effects.
  // returns a writer that emits the states on the output side and the new
  // messages on the write side.
  implicit class MProcToZthulhu[Z](source: MProc)
  (implicit log: Logger)
  {
    type Trans = (Z, Message) ⇒ Maybe[(Z, Effect)]

    def fsm(initial: Z, transition: Trans) = {
      source
        .pipe(transLoop(initial)(transition)(fatalTransition))
        .flatMapO(handleResult)
    }
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
      .attempt(t ⇒ emit(LogFatal("performing effect", t).publish.fail))
      .mergeO
      .mapO {
        case scalaz.Success(trans) ⇒
          log.debug(s"task succeeded with ${trans.show}")
          List(trans)
        case scalaz.Failure(transs) ⇒
          log.debug(s"task failed with ${transs.show}")
          transs.toList
      }
      .pipeO(process1.unchunk)
  }

  implicit def amendZthulhuWithEmptyEffects(z: Zthulhu): TransitResult =
    (z, halt)

  implicit class TransitResultOps(r: TransitResult)
  {
    def <<[A: StateEffect](e: A): TransitResult = {
      (r.head, r.last ++ (e: Effect))
    }

    def <<(prc: Parcel): TransitResult = {
      (r.head, r.last ++ (prc.success: Effect))
    }
  }

  implicit class ZthulhuOps(z: Zthulhu)
  {
    def <<[A: StateEffect](e: A) = (z: TransitResult) << e
    def <<(prc: Parcel) = (z: TransitResult) << prc
  }
}
