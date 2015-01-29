package tryp.droid.util

import com.github.nscala_time.time.Imports._

trait JodaExt
{
  implicit class `DateTime extensions`(dt: DateTime) {
    def newerThan(m: Long) = {
      (dt + m.toInt.minutes) > DateTime.now
    }
  }
}
