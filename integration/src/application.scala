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
with state.core.ToIOStateOps
{
  def trans: Transitions = {
    case UpdateInt(strings) => {
      Update(strings).local :: HNil
    }
    case CreateAdapter =>
      adapter.map(SetAdapter(_)).runner :: HNil
  }
}

class IntAppState
extends ExtMVAppState
{
  val intView = IntView.aux

  def loopCtor = Loop.fast(intView :: androidCells, Nil)
}

class IntApplication
extends StateApplication
{
  lazy val state = new IntAppState
}
