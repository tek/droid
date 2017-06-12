package tryp
package droid
package unit

import view.io

@tryp.annotation.Slick
object DbSpecSchema
{
  case class Alpha(a: String)
}

trait DbSpecData
{
  import AppState._
  import MainViewMessages._
  import Db._
  import AIOOperation.exports._

  implicit def dbi: DbInfo

  object DB
  extends DbSpecSchema.Slick()

  class Marker(c: Context)
  extends TextView(c)

  case class TestAction(s: String)
  extends Message

  class DbView
  extends ActivityAgent
  {
    lazy val viewMachine = new ViewMachine {
      lazy val label = w[Marker]

      lazy val layout = l[FrameLayout](label)

      def admit: Admission = {
        case ContentViewReady(_) =>
          _ << DB.Alpha.one.map { a =>
            val newText = a.headOption map(_.a) getOrElse("invalid")
            (label.v >>- io.text.text(newText)).void.main
          }
      }
    }
  }
}

class DbSpec
extends StateAppSpec
with DbSpecData
{
  def is = s2"""
  execute a db action $dbAction
  """

  def initialAgent = new DbView

  def fix = "fixture_string"

  implicit lazy val dbi = stateApp.dbi

  override def before = {
    super.before
    DB.initDb()
    DB.Alpha.create(fix).!!
  }

  def dbAction = activity willContain text(fix)
}
