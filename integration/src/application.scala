package tryp
package droid
package integration

import java.util.concurrent._

import fs2.async.signalOf

import droid.state.StateApplicationAgent
import droid.state.AppState.SetActivity

object CreateAgents
extends Logging
{
  def apply(root: Agent)
  (implicit strat: Strategy, x: ExecutorService): Stream[Task, Agents] = {
    Stream.eval(signalOf[Task, Option[Agents]](None))
      .flatMap { sig =>
        Agents(Stream(root)).run
          .evalMap {
            case Right(ag) =>
              sig.set(Some(ag))
            case Left(m) =>
              Task(m)
          }
          .run
          // .unsafeRunAsync { _ => () }
          .infraFork("fork agents")
        sig.discrete
      }
      .collect { case Some(ag) => ag }
      .take(1)
  }
}

class IntApplication
extends android.app.Application
with StateApplication
with Logging { ia =>
  def name = "app"

  lazy val root =
    new StateApplicationAgent {
      def name = "state_app"
      def agents = Nil
      override def initialAgent = Some(new Simple)
      def strat = ia.strategy
    }

  lazy val agents =
    CreateAgents(root)
      .infraRunLastFor("initialize agents", 10.seconds)
      .getOrElse(sys.error("agents haven't initialized"))

  override def onCreate(): Unit = {
    agents
    super.onCreate()
  }

  def setActivity(act: Activity) = {
    dbg("setActivity")
    agents.send(SetActivity(act))
  }
}
