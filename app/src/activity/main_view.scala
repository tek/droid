// package tryp
// package droid

// import cats._
// import cats.syntax.apply._

// import state._
// import state.core._
// import view._
// import view.core._

// import MainViewMessages._

// trait FreeMainViewMachine
// extends Machine
// {
//   import AppState._

//   override def description = "main view state"

//   val admit: Admission = {
//     case LoadMUi(ui) => loadUi(ui)
//     case LoadFragment(fragment, tag) => loadFragment(fragment, tag)
//     case ContentLoaded(view) => contentLoaded(view)
//     case Back => back
//   }

//   def loadUi(ui: Ui[View]): Transit = {
//     case s =>
//       s
//   }

//   def loadFragment(fragment: () => Fragment, tag: String): Transit = {
//     case s =>
//       s << ContextTask(
//         _.transitionFragment(FragmentBuilder(fragment, RId.content, Some(tag)))
//           .toResult
//       )
//   }

//   def contentLoaded(view: Ui[View]): Transit = {
//     case s => s << ContextTask(ctx =>
//       LogInfo(s"Loaded content view:\n${ctx.showViewTree(view.get)}")
//     )
//   }

//   def back: Transit = {
//     case s =>
//       s << Ui { nativeBack() }
//   }

//   def nativeBack(): Unit = ()
// }

// trait MainView
// extends ActivityBase
// with Transitions
// with FreeActivityAgent
// {
//   mainView: Akkativity =>

//     import macroid.FullDsl._

//     val content = slut[FrameLayout]

//     lazy val mainViewMachine = new FreeMainViewMachine {
//       override def handle = "mainview"
//       override def description = "main view state"
//       override def nativeBack() = mainView.nativeBack()
//     }

//     override def machines = mainViewMachine %:: super.machines

//     def setContentView(v: View)

//     abstract override def onCreate(state: Bundle) {
//       super.onCreate(state)
//       mainActor
//       initView()
//     }

//     def initView() = {
//       setContentView(Ui.get(mainLayout))
//     }

//     def mainLayout = contentLayout

//     def contentLayout: Ui[ViewGroup] = {
//       val tw = List(bgCol("main"), Some(metaName("root frame"))).flatten
//       attachRoot(FL(tw: _*)(
//         l[FrameLayout]() <~ content <~ RId.content <~ metaName("content frame")))
//     }

//     def loadFragment(fragment: Fragment) = {
//       val f = frag(this, fragment, RId.content)
//       send(LoadMUi(f))
//     }

//     def loadShowFragment[A <: SyncModel: ClassTag]
//     (model: A, ctor: () => ShowFragment[A]) {
//       send(LoadMUi(showFrag(this, model, ctor, RId.content)))
//     }

//     def contentLoaded() {}

//     // This is the entry point for back actions, when the actual back key or
//     // the drawer toggle had been pressed. Any manual back initiation should also
//     // call this.
//     // The main actor can have its own back stack, so it is asked first. If it
//     // declines or the message cannot be dispatched, it is sent back here and
//     // dispatched to back() below.
//     override def onBackPressed() {
//       mainActor ! Messages.Back()
//     }

//     def back() {
//       send(Back)
//     }

//     def nativeBack() {
//       super.onBackPressed()
//     }

//     def canGoBack = this.backStackNonEmpty

//     lazy val mainActor = createActor(MainActor.props)._2

//     def showDetails(data: Model) {}
// }
