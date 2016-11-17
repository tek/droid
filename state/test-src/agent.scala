package tryp
package droid
package state

import tryp.state._

import org.specs2._, specification._, matcher._, concurrent._

import scalaz._, Scalaz._, scalaz.concurrent._, stream.async

import shapeless.HNil

import droid.state._

case object Go
extends Message

trait State1
extends Machine
{
  override def handle = "state1"

  lazy val output = async.signalOf(-1)

  def admit: Admission = {
    case Go => {
      case s =>
        s << output.set(0)
    }
  }
}

class AgentSpec
extends tryp.Spec
with CachedPool
{
  def name = "spec"

  override def retries = 2

  def is = sequential ^ s2"""
  start a subagent dynamically $subAgent
  publish to a subagent $publish
  """

  lazy val root = new RootAgent
  {
    override def handle = "root"
  }

  lazy val ag1 = new Agent {
    override def handle = "ag1"

    lazy val state = new State1 {}

    override def machines = state :: super.machines
  }

  def subAgent = {
    root.runAgent()
    root.startSubAgent(ag1, 5 seconds) will_== true
  }

  def publish = {
    root.publish1(Go)
    ag1.state.output.get will_== 0
  }
}
