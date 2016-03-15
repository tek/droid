package tryp
package droid
package state

import org.specs2._, specification._, matcher._, concurrent._

import scalaz._, Scalaz._, scalaz.concurrent._, stream.async

import shapeless.HNil

import droid.state._

case object Go
extends Message

trait State1
extends Machine
{
  def handle = "state1"

  lazy val output = async.signalOf(-1)

  def admit: Admission = {
    case Go => {
      case s =>
        s << output.set(0)
    }
  }
}

class AgentSpec
extends Specification
with tryp.Matchers
with CachedPool
{
  def name = "spec"

  def is = sequential ^ s2"""
  start a subagent dynamically $subAgent
  publish to a subagent $publish
  """

  lazy val root = new RootAgent
  {
    def handle = "root"
  }

  lazy val ag1 = new Agent {
    def handle = "ag1"

    lazy val state = new State1 {}

    override def machines = state %:: super.machines
  }

  def subAgent = {
    root.runAgent()
    root.startSubAgent(ag1, 5 seconds) will_== true
  }

  def publish = {
    root.publishOne(Go)
    ag1.state.output.get will_== 0
  }
}
