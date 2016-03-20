package tryp
package droid
package unit

import shapeless._

import android.widget._
import android.support.v7.widget.RecyclerView

import droid.state._
import view._

trait SpecViewMachine
extends ViewMachine
{
  import io.text._
  import iota._

  import android.view.ViewGroup.LayoutParams._

  lazy val search = w[AutoCompleteTextView] >>= large[AutoCompleteTextView]

  lazy val layoutIO = {
    l[FrameLayout](
      (l[RelativeLayout](search :: HNil) >>=
        lp[RelativeLayout](MATCH_PARENT, MATCH_PARENT)
        ) :: HNil
      )
    }
}

case class SpecFragment()
extends VSTrypFragment
{
  lazy val viewMachine: SpecViewMachine = new SpecViewMachine {}
}

class SpecActivity1
extends SpecActivity
{
  override def frag = SpecFragment.apply
}


abstract class SpecBase
extends ActivitySpec[SpecActivity1]
{
  def activityClass = classOf[SpecActivity1]
}
