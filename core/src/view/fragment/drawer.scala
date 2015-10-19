package tryp

import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.contrib.TextTweaks._

import tryp.tweaks.Recycler._
import tryp.res._
import tryp.Macroid._

class DrawerFragment
extends TrypFragment
{
  val drawerView = slut[RecyclerView]

  override val actors = Seq(DrawerActor.props)

  def layout(state: Bundle) = {
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
