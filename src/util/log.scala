package tryp.droid.meta

import scala.collection.mutable.Buffer

import akka.actor.ActorSelection

import tryp.droid.Messages

import tryp.core.util._
import tryp.core.meta._

trait AndroidLog
extends LogInterface
{
  def tg = AndroidLog.tag

  abstract override def d(message: String) = android.util.Log.d(tg, message)

  abstract override def i(message: String) = android.util.Log.i(tg, message)

  abstract override def w(message: String) = android.util.Log.w(tg, message)

  abstract override def e(message: String) = android.util.Log.e(tg, message)

  abstract override def e(message: String, t: Throwable) = {
    android.util.Log.e(tg, message, t)
  }
}

object AndroidLog
extends LogBase
with AndroidLog
{
  var tag = "tryp"

  override def d(m: String) = super.d(m)
  override def i(m: String) = super.i(m)
  override def w(m: String) = super.w(m)
  override def e(m: String) = super.e(m)
  override def e(m: String, t: Throwable) = super.e(m, t)
}

trait InternalLog
extends LogInterface
{

  var buffer = Buffer[String]()

  var actor: Option[ActorSelection] = None

  def log(message: String) {
    buffer += tryp.core.Time.nowHms + " -- " + message
    actor foreach { _ ! Messages.Log(message) }
  }

  abstract override def d(message: String) = {
    log(message)
    super.d(message)
  }

  abstract override def i(message: String) = {
    log(message)
    super.i(message)
  }

  abstract override def w(message: String) = {
    log(message)
    super.w(message)
  }

  abstract override def e(message: String) = {
    log(message)
    super.e(message)
  }

  abstract override def e(message: String, t: Throwable) = {
    log(message)
    super.e(message)
  }
}

object InternalLog
extends LogBase
with InternalLog
{
  override def d(m: String) = super.d(m)
  override def i(m: String) = super.i(m)
  override def w(m: String) = super.w(m)
  override def e(m: String) = super.e(m)
  override def e(m: String, t: Throwable) = super.e(m, t)
}

object DebugLog
extends LogBase
with AndroidLog
with InternalLog
{
  override def d(m: String) = super.d(m)
  override def i(m: String) = super.i(m)
  override def w(m: String) = super.w(m)
  override def e(m: String) = super.e(m)
  override def e(m: String, t: Throwable) = super.e(m, t)
}
