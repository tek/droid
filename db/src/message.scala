package tryp
package droid
package db

import droid.state.FromContext

object FromContextDb
{
  implicit def instance_FromContext_DbInfo: FromContext[DbInfo] =
    new FromContext[DbInfo] {
      def summon(c: Context) = Db.fromContext(c)
    }
}
