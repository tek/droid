package tryp.droid.util

import scala.math.min

import java.util.{Timer, TimerTask}

import Control._

object Time {
  def now = System.currentTimeMillis / 1000

  def hms(seconds: Long): String = {
    val intervals = diffIntervalsHms(seconds)
    val hour = intervals("hour")
    val minute = intervals("minute")
    val second = intervals("second")
    f"$hour%02d:$minute%02d:$second%02d"
  }

  def zeroHms = "00:00:00"

  def nowHms = hms(now)

  def diffIntervalsHms(total: Long): Map[String, Long] = {
    val hours = total / 3600
    val rest = total % 3600
    val minutes = rest / 60
    val seconds = rest % 60
    Map("hour" -> hours, "minute" -> minutes, "second" -> seconds)
  }
}

class Ticker(period: Double)(callback: => Unit)
{
  val timer = new Timer
  var running = false
  var task: Option[TimerTask] = None

  def start {
    try {
      val t = new TimerTask { def run { callback } }
      val p = (period <= 0) ? 1.0 / period
      timer.scheduleAtFixedRate(t, 0, (p * 1000).toInt)
      running = true
      task = Some(t)
    }
    catch {
      case ex: IllegalStateException =>
        Log.e(s"Couldn't start Ticker: ${ex}")
    }
  }

  def stop {
    task foreach { t ⇒
      t.cancel
      task = None
    }
    running = false
  }
}
