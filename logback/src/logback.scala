package tryp
package droid

import org.log4s.getLogger
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{LoggerContext, Logger => LLogger, Level}
import ch.qos.logback.core.util.StatusPrinter

object LogbackDebug
{
  def printStatus() = {
    StatusPrinter.print(
      LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext])
  }
}

object Logging
extends tryp.Logging
{
  lazy val DEBUG = ch.qos.logback.classic.Level.DEBUG

  def overrideLevel(name: String, level: Level) = {
    getLogger(name).logger match {
      case l: LLogger => l.setLevel(level)
      case a => logd.error(s"tried to set level on invalid logger $a")
    }
  }

  def debugTryp() = overrideLevel("tryp", DEBUG)
}
