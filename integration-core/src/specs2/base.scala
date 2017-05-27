package tryp
package droid
package integration

import org.specs2._

import scalaz.Scalaz, Scalaz.{ToTraverseOps, ToFoldableOps}, scalaz.std.list._

import main.Arguments
import reporter.LineLogger.consoleLogger
import runner.{Runner, ClassRunner}
import specification.core.{Env, SpecificationStructure}
import specification.process.StatisticsRepository

object runSpec
extends ClassRunner
{
  def apply(specifications: SpecificationStructure *)
  (implicit arguments: Arguments = Arguments()) = {
    val loader = Thread.currentThread.getContextClassLoader
    val env = Env(
      arguments = arguments,
      lineLogger = consoleLogger,
      statsRepository = (arguments: Arguments) => StatisticsRepository.memory
      )
    val actions =
      ToTraverseOps(specifications.toList.map(report(env)))
        .sequenceU
        .map(a => ToFoldableOps(a).foldMap(identity _))
    try Runner.execute(actions, arguments, exit = false)(env.executionContext)
    finally env.shutdown
  }
}

trait SpecsBase
extends SpecificationLike
{ _: IntegrationSpec[_ <: Activity] =>
  def androidInstrumentationTest() = {
    activity
    runSpec(this)
  }
}
