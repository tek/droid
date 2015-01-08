package tryp.droid

import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.contrib.TextTweaks._

import tryp.droid.tweaks.Recycler._
import tryp.droid.res._
import tryp.droid.Macroid._

class DrawerFragment
extends TrypFragment
{
  override def onCreate(state: Bundle) {
    super.onCreate(state)
  }

  lazy val drawerAdapter = new DrawerAdapter(actor)

  override def macroidLayout(state: Bundle) = {
    LL(vertical, bgCol("main"))(
      w[RecyclerView] <~ recyclerAdapter(drawerAdapter) <~
        linearLayoutManager <~ divider
    )
  }
}
