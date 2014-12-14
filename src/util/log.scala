package tryp.droid.util

class LogBase {
  var tag = "tryp"

  def d(message: String) = 0

  def p(message: Any) = 0

  def i(message: String) = 0

  def w(message: String) = 0

  def e(message: String) = 0

  def e(message: String, t: Throwable) = 0

  def t(message: String) = 0
}

object NullLog extends LogBase

object Log extends LogBase
{
  override def d(message: String) = android.util.Log.d(tag, message)

  override def p(message: Any) = {
    if (message == null)
      d("null")
    else
      d(message.toString)
  }

  override def i(message: String) = android.util.Log.i(tag, message)

  override def w(message: String) = android.util.Log.w(tag, message)

  override def e(message: String) = android.util.Log.e(tag, message)

  override def e(message: String, t: Throwable) = {
    android.util.Log.e(tag, message, t)
  }

  override def t(message: String) = {
    val output = "----- " + message + ": " + Time.nowHms
    d(output)
  }
}
