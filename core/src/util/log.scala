package tryp.droid.meta

import akka.actor.ActorSelection

import tryp.Messages

import tryp.util._
import tryp.meta._

trait AndroidLog
extends LogInterface
{
  def tg = AndroidLog.tag

  abstract override def dImpl(message: String) =
    android.util.Log.d(tg, message)

  abstract override def iImpl(message: String) =
    android.util.Log.i(tg, message)

  abstract override def wImpl(message: String) =
    android.util.Log.w(tg, message)

  abstract override def eImpl(message: String) =
    android.util.Log.e(tg, message)
}

object AndroidLog
extends LogBase
with AndroidLog
{
  var tag = "tryp"

  override def dImpl(m: String) = super.dImpl(m)
  override def iImpl(m: String) = super.iImpl(m)
  override def wImpl(m: String) = super.wImpl(m)
  override def eImpl(m: String) = super.eImpl(m)
}

trait InternalLog
extends LogInterface
{

  def log(message: String) {
    InternalLog.buffer += tryp.Time.nowHms + " -- " + message
    InternalLog.actor foreach { _ ! Messages.Log(message) }
  }

  abstract override def dImpl(message: String) = {
    log(message)
    super.dImpl(message)
  }

  abstract override def iImpl(message: String) = {
    log(message)
    super.iImpl(message)
  }

  abstract override def wImpl(message: String) = {
    log(message)
    super.wImpl(message)
  }

  abstract override def eImpl(message: String) = {
    log(message)
    super.eImpl(message)
  }
}

object InternalLog
extends LogBase
with InternalLog
{
  var buffer = Buffer[String]()

  var actor: Option[ActorSelection] = None

  override def dImpl(m: String) = super.dImpl(m)
  override def iImpl(m: String) = super.iImpl(m)
  override def wImpl(m: String) = super.wImpl(m)
  override def eImpl(m: String) = super.eImpl(m)
}

object DebugLog
extends LogBase
with AndroidLog
with InternalLog
{
  override def dImpl(m: String) = super.dImpl(m)
  override def iImpl(m: String) = super.iImpl(m)
  override def wImpl(m: String) = super.wImpl(m)
  override def eImpl(m: String) = super.eImpl(m)
}
