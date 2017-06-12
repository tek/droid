package tryp
package droid
package integration

import android.support.v7.app.AppCompatActivity

import iota._

import tryp.state.annotation.cell
import recycler.{CommRV, StringElement, RA, DefaultRV}
import state.StatePool._
import state.annotation.{viewCell, cellModel}
import recycler.annotation.rvCell

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

case class Clicked(model: String)
extends Message

case class SomeMain(container: FrameLayout)
extends ViewTree[FrameLayout]

case class SomeModel(a: Int)

@viewCell[SomeMain]
@cellModel[SomeModel]
object SomeView
{
}

@rvCell[String, StringElement]
object IntView
extends CommRV
with DefaultRV
with MainViewCell
with state.core.ToAIOStateOps
{
  def bind(comm: Comm)(tree: StringElement, model: String): Unit = {
    tree.label.setText(model)
    tree.container.onClick(comm.send(Clicked(model)))
  }

  def trans: Transitions = {
    case UpdateInt(strings) => {
      Update(strings).local :: HNil
    }
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
