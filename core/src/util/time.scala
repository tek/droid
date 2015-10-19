package tryp

import rx._
import rx.ops._

import java.util.{Timer, TimerTask}

case class Ticker(seconds: Rx[Double])(callback: ⇒ Unit)
{
  val millis = Rx { (((seconds() <= 0) ? 1.0 | seconds()) * 1000).toInt }
  val timer = new Timer
  var running = false
  var task: Option[TimerTask] = None

  def start() {
    try {
      val t = new TimerTask { def run { callback } }
      timer.scheduleAtFixedRate(t, 0, millis())
      running = true
      task = Some(t)
    }
    catch {
      case ex: IllegalStateException ⇒
        Log.e(s"Couldn't start Ticker: ${ex}")
    }
  }

  def stop() {
    task foreach { t ⇒
      t.cancel
      task = None
    }
    running = false
  }

  def restart() {
    stop()
    start()
  }
}
