package tryp.droid

import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.contrib.TextTweaks._

import tryp.droid.tweaks.Recycler._
import tryp.droid.res._
import tryp.droid.Macroid._

// TODO either supply a default ctor or make android retainInstance
class DrawerFragment(navigation: Navigation)
extends TrypFragment
{
  override implicit def resourceNamespace = PrefixResourceNamespace("drawer")

  lazy val adapter = new DrawerAdapter(core, navigation)

  override def macroidLayout(state: Bundle) = {
    LL(vertical, bgCol("main"))(
      w[RecyclerView] <~ recyclerAdapter(adapter) <~ linear <~ divider
    )
  }

  def navigated() {
    adapter.notifyDataSetChanged
  }
}

case class DefaultDrawerFragment(navigation: Navigation)
extends DrawerFragment(navigation)
