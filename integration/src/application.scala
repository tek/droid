package tryp
package droid
package integration

import android.support.v7.app.AppCompatActivity

import shapeless._

import fs2.async

import iota._

import tryp.state.annotation._

case object Msg
extends Message

case object Dummy
extends Message

class IntStateActivity
extends StateActivity
{
}

case class IntMain(container: FrameLayout, tv: TextView)
extends ViewTree[FrameLayout]
{
  tv.setText("success")
  tv.setTextSize(50)

  override def toString = "IntMain"
}

@cell
object IntView
extends ViewCell
{
  type CellTree = IntMain

  def infMain = inflate[IntMain]

  def narrowTree(tree: state.AnyTree) = tree match {
    case t: IntMain => Some(t)
    case _ => None
  }

  def trans: Transitions = {
    case Msg => HNil
  }
}

class IntAppState
extends AppState
{
  import state.StatePool._

  def loopCtor = Loop.cells(androidCells :: (IntView.aux :: HNil) :: HNil, Nil)
}

class IntApplication
extends StateApplication
{
  lazy val state = new IntAppState
}
