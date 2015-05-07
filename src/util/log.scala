package tryp.droid.meta

import scala.collection.mutable.Buffer

import akka.actor.ActorSelection

import tryp.droid.Messages

import tryp.core.util._
import tryp.core.meta._

class AndroidLog extends LogBase
{
  var tag = "tryp"

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

trait InternalLog extends LogBase
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

object InternalLog extends InternalLog

object DebugLog
extends AndroidLog
with InternalLog
