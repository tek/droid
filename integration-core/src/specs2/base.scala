package tryp
package droid
package integration

import scalaz.Scalaz, Scalaz.{ToTraverseOps, ToFoldableOps}, scalaz.std.list._

import org.specs2.main.Arguments
import org.specs2.reporter.LineLogger.consoleLogger
import org.specs2.runner.{Runner, ClassRunner}
import org.specs2.specification.core.{Env, SpecificationStructure, ImmutableSpecificationStructure}
import org.specs2.specification.process.StatisticsRepository

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
extends ImmutableSpecificationStructure
{ _: IntegrationSpec[_ <: Activity] =>
  def androidInstrumentationTest() = {
    activity
    runSpec(this)
  }
}
