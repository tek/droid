package tryp
package droid
package state

import scalaz._, Scalaz._

import shapeless._
import shapeless.tag.@@

import stream.async
import Process._

import Zthulhu._

// TODO
// merge topic with accept, providing input of only those types specified as
// params
// for export, try to create a macro that divides the messages at compile-time,
// maybe via class annotation
abstract class Machine
(implicit messageTopic: MessageTopic @@ To)
extends Logging
with CachedStrategy
{
  def cachedPool = Zthulhu

  val S = Zthulhu

  def admit: Admission

  private[this] val internalMessageIn = async.unboundedQueue[Message]

  private[this] val externalMessageIn = async.unboundedQueue[Message]

  private[this] def messageIn = {
    internalMessageIn.dequeue.logged(s"$description internal")
      .merge(messageTopic.subscribe.logged(s"$description external"))
  }

  val messageOut = async.unboundedQueue[Message]

  private[this] val running = async.signalOf(true)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  val current = async.signalUnset[Zthulhu]

  private[this] val idle: Process[Task, Boolean] = size map(_ == 0)

  protected def preselect: Preselection = {
    case m: InternalMessage ⇒ internalMessage
    case m: Message ⇒ admit
  }

  protected def uncurriedTransitions(z: Zthulhu, m: Message) = {
    preselect(m)
      .orElse(unmatchedMessage)
      .lift(m)
      .getOrElse(unmatchedState(m))
      .lift(z)
      .toMaybe
  }

  private[this] def fsmProc(initial: BasicState)
  : Process[Task, Message] = {
    term.discrete
      .merge(idle.when(quit.discrete))
      .wye(messageIn)(stream.wye.interrupt)
      .fsm(initial, uncurriedTransitions)
      .forkW(current.pipeIn)
      .separateMap(_.publish)(_.message)
      .forkW(internalMessageIn.enqueue)
  }

  val debugStates = false

  def run(initial: BasicState = Pristine)
  : Process[Task, Message] = {
    if (debugStates)
      current.discrete
        .map(z ⇒ log.debug(z.toString))
        .infraRunAsync("debug state printer")
    fsmProc(initial)
      .onComplete { running.setter(false).flatMap(_ ⇒ halt) }
  }

  def sendP(msg: Message) = emit(msg).to(internalMessageIn.enqueue)

  def send(msg: Message) = {
    sendAll(msg.wrapNel)
  }

  def sendAll(msgs: NonEmptyList[Message]) = {
    internalMessageIn.enqueueAll(msgs.toList) !? "enqueue state messages"
  }

  // kill the state machine by
  // the 'term' signal is woven into the main process using the wye combinator
  // 'interrupt', which listens asynchronously and terminates instantly
  def kill() = {
    term.set(true) !? "set signal term"
    externalMessageIn.kill !? "kill external message queue"
    internalMessageIn.kill !? "kill internal message queue"
  }

  // gracefully shut down the state maching
  // the 'quit' signal uses the deterministic tee combinator 'until', which is
  // only emitted when the 'idle' signal is true
  // 'idle' is set when no messages are queued
  // *> combines the two Task instances via Apply.apply2
  // roughly equivalent to a flatMap(_ ⇒ b)
  def join() = {
    log.trace(s"terminating $this")
    (quit.set(true) *> finished.run)
      .infraRunFor("wait for finished signal", 20 seconds)
  }

  def finished = running.continuous.exists(!_)

  private[this] def size = {
    externalMessageIn.size.discrete
      .yipWith(internalMessageIn.size.discrete)(_ + _)
      .take(1)
  }

  private[this] def waitingTasks = {
    size.runLast.attemptRun | None | 0
  }

  def description = s"$handle state"

  def handle: String

  override def toString = s"$description ($waitingTasks waiting)"

  override val loggerName = s"state.$handle".some

  lazy val internalMessage: Admission = {
    case SetInitialState(s) ⇒ setInitialState(s)
  }

  def setInitialState(s: BasicState): Transit = {
    case S(Pristine, d) ⇒ S(s, d)
  }

  lazy val unmatchedMessage: Admission = {
    case m if debugStates ⇒ {
      case s ⇒
        log.debug(s"unmatched message at $s: $m")
        s
    }
  }

  def unmatchedState(m: Message): Transit = {
    case s if debugStates ⇒
      log.debug(s"unmatched state $s: $m")
      s
  }

  implicit def defaultPublishFilter[A <: Message] = new PublishFilter[A] {
    def allowed = false
  }
}
