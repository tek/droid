package tryp.droid.meta

class LogBase {
  var tag = "tryp"

  def d(message: String) { }

  def p(message: Any) = {
    if (message == null)
      d("null")
    else
      d(message.toString)
  }

  def i(message: String) { }

  def w(message: String) { }

  def e(message: String) { }

  def e(message: String, t: Throwable) { }

  def t(message: String) { }
}

object NullLog extends LogBase

object Log extends LogBase
{
  override def d(message: String) = android.util.Log.d(tag, message)

  override def i(message: String) = android.util.Log.i(tag, message)

  override def w(message: String) = android.util.Log.w(tag, message)

  override def e(message: String) = android.util.Log.e(tag, message)

  override def e(message: String, t: Throwable) = {
    android.util.Log.e(tag, message, t)
  }

  override def t(message: String) = {
    val output = "----- " + message + ": " + tryp.droid.Time.nowHms
    d(output)
  }
}

object StdoutLog extends LogBase {
  override def d(message: String) = println(message)

  override def i(message: String) = println(message)

  override def w(message: String) = println(message)

  override def e(message: String) = println(message)

  override def e(message: String, t: Throwable) = println(message)

  override def t(message: String) = println(message)
}
