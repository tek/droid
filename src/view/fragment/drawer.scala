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
  val drawerView = slut[RecyclerView]

  override def macroidLayout(state: Bundle) = {
    FL(bgCol("main"))(
      w[RecyclerView] <~ drawerView <~ linear <~ divider
    )
  }

  def navigated() = {
    drawerView <~ dataChanged
  }

  def setNavigation(nav: Navigation) = {
    drawerView <~ recyclerAdapter(new DrawerAdapter(nav))
  }

  override val name = "Drawer"
}

case class DefaultDrawerFragment()
extends DrawerFragment
