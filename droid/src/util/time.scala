package tryp.droid.util

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
