// package tryp
// package droid
// package state

// import IOOperation.exports._

// import android.view.Gravity

// object ToolbarMachineData
// {
// }

// trait ToolbarMachine
// extends IOViewMachine[ViewGroup]
// {
//   import ViewMachine._
//   import ToolbarMachineData._
//   import view.io.misc._

//   val t = theme.dimension("actionBarSize")
//     .map(a => T.minHeight(a.toInt)).toOption

//   def belowToolbarLayout: StreamIO[_ <: View, Context]

//   lazy val toolbar =
//     l[Toolbar](
//       w[FrameLayout] >>=
//         iota.effect.id[FrameLayout](iota.effect.Id.toolbar)) >>-
//         bgCol("toolbar") >>-
//         titleColor("toolbar_text")

//   lazy val layout = {
//     l[FrameLayout](
//       l[LinearLayout](
//         toolbar,
//         belowToolbarLayout >>= iota.effect.lp(MATCH_PARENT, MATCH_PARENT)
//       )
//     ).widen[ViewGroup] >>- fitsSystemWindows
//   }

//   override def machinePrefix = super.machinePrefix :+ "toolbar"

//   def admit: Admission = {
//     case AppState.ContentViewReady(_) => {
//       case s => s
//     }
//       val e = toolbar.v
//         .map(t => actAs[ActionBarActivity, Unit](_.setSupportActionBar(t)))
//       _ << IOOperation
//         .instance_StateEffect_ViewStream[IO[Effect, Activity], Context]
//         .stateEffect(e)
//   }
// }
