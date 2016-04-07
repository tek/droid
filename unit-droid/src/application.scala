package tryp
package droid
package unit

class SpecStateApplication
extends android.app.Application
with state.StateApplication
{
  val dbi = Db.fromDbName(DbName(Random.string(10)))

  override def dbInfo = Some(dbi)
}
