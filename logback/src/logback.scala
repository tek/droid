package tryp
package droid

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter

object LogbackDebug
{
  def printStatus() = {
    StatusPrinter.print(
      LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext])
  }
}
