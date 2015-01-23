package tryp.droid.meta

import scala.collection.mutable.Buffer

import akka.actor.ActorSelection

import tryp.droid.Messages

class LogBase {
  var tag = "tryp"

  def d(message: String) { }

  def p(message: Any) = {
    if (message == null)
      i("null")
    else
      i(message.toString)
  }

  def i(message: String) { }

  def w(message: String) { }

  def e(message: String) { }

  def e(message: String, t: Throwable) { }

  def t(message: String) { }
}

object NullLog extends LogBase

class AndroidLog extends LogBase
{
  override def d(message: String) = android.util.Log.d(tag, message)

  override def i(message: String) = android.util.Log.i(tag, message)

  override def w(message: String) = android.util.Log.w(tag, message)

  override def e(message: String) = android.util.Log.e(tag, message)

  override def e(message: String, t: Throwable) = {
    android.util.Log.e(tag, message, t)
  }

  override def t(message: String) = {
    i(s"[${tryp.droid.Time.nowHms}] ${message}")
  }
}

object AndroidLog extends AndroidLog

object StdoutLog extends LogBase {
  override def d(message: String) = println(message)

  override def i(message: String) = println(message)

  override def w(message: String) = println(message)

  override def e(message: String) = println(message)

  override def e(message: String, t: Throwable) = println(message)

  override def t(message: String) = println(message)
}

object DebugLog extends AndroidLog
{
  var buffer = Buffer[String]()

  var actor: Option[ActorSelection] = None

  def log(message: String) {
    buffer += tryp.droid.Time.nowHms + " -- " + message
    actor foreach { _ ! Messages.Log(message) }
  }

  override def d(message: String) = {
    log(message)
    super.d(message)
  }

  override def i(message: String) = {
    log(message)
    super.i(message)
  }

  override def w(message: String) = {
    log(message)
    super.w(message)
  }

  override def e(message: String) = {
    log(message)
    super.e(message)
  }

  override def e(message: String, t: Throwable) = {
    log(message)
    super.e(message)
  }

  override def t(message: String) = {
    log(message)
    super.t(message)
  }
}
