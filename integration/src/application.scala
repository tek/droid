package tryp
package droid
package integration

import android.support.v7.app.AppCompatActivity

import tryp.state.annotation.cell
import recycler.StringRV
import state.StatePool._

case object Msg
extends Message

case object Dummy
extends Message

class IntStateActivity
extends StateActivity
{
  def initialMessages = Nil
}

case class UpdateInt(strings: List[String])
extends Message

@cell
object IntView
extends StringRV
with MainViewCell
{
  def trans: Transitions = {
    case UpdateInt(strings) => {
      Update(strings).local :: HNil
    }
  }
}

class IntAppState
extends ExtMVAppState
{
  def loopCtor = Loop.cells(androidCells :: (IntView.aux :: HNil) :: HNil, Nil)
}

class IntApplication
extends StateApplication
{
  lazy val state = new IntAppState
}
