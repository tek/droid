// package tryp.droid

// import android.support.v7.widget.RecyclerView

// import macroid.FullDsl._
// import macroid.contrib.TextTweaks._

// import tryp.droid.tweaks.Recycler._
// import tryp.droid._
// import tryp.droid.Macroid._

// class DrawerFragment
// extends TrypFragment
// with PlusInterfaceAccess
// {
//   val drawerView = slut[RecyclerView]

//   override def handle = "drawer"

//   override val actors = Seq(DrawerActor.props)

//   def layout(state: Bundle) = {
//     FL(bgCol("main").toList: _*)(
//       w[RecyclerView] <~ drawerView <~ linear <~ divider
//     )
//   }

//   def navigated() = {
//     drawerView <~ dataChanged
//   }

//   def setNavigation(nav: Navigation) = {
//     drawerView <~ recyclerAdapter(new DrawerAdapter(nav))
//   }

//   override val name = "Drawer"
// }

// case class DefaultDrawerFragment()
// extends DrawerFragment
