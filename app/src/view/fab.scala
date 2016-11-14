//   def fabCorner(icon: String)(contentView: => Ui[View]) = {
//     val geom = rlp(↧, ↦) + margin(right = 16 dp, bottom = 48 dp)
//     RL(rlp(↔, ↕), metaName("fab corner container"))(
//       content(contentView) <~ metaName("content view"),
//       progressUi <~ geom <~ metaName("progress indicator"),
//       fabUi(icon) <~ geom <~ metaName("fab")
//     )
//   }

//   def fabBetween(icon: String, parallax: Boolean = false)
//   (headerView: Ui[View], contentView: Ui[View]) =
//   {
//     val marg = res.dimen("fab_margin_normal_minus").getOrElse((16 dp).toFloat)
//     val geom = rlp(↦, alignBottom(RId.header)) +
//       margin(right = 16 dp, bottom = marg.toInt)
//     val contentParams = rlp(parallax ? ↥ | below(RId.header))
//     RL(rlp(↔, ↕), metaName("fab between container"))(
//       content(contentView) <~ contentParams <~ metaName("content view"),
//       header(
//         RL(rlp(↔, ↕))(
//           headerView <~ metaName("fab header")
//         ) <~ bgCol("header") <~ metaName("fab header container")
//       ) <~ RId.header <~ rlp(↥, ↔, Height(headerHeight)),
//       progressUi <~ geom <~ metaName("progress indicator"),
//       fabUi(icon) <~ geom <~ metaName("fab")
//     )
//   }

//   def progressUi = w[ProgressBar] <~ indeterminate <~ hide <~
//     res.dimen("fab_width").map(a => Width(a.toInt)).toOption <~ whore(progress)

//   def fabUi(icon: String) = {
//     fab() <~
//       image(icon) <~
//       imageScale(ImageView.ScaleType.CENTER) <~
//       Fab.colors("colorAccentStrong", "colorAccent") <~
//       On.click { Ui(fabClick()) }
//   }

//   // Runs 'task' while changing the fab to a circular progress indicator. After
//   // completion, 'snack' is shown as a toast, if nonempty.
//   // TODO queue into Process
//   def fabAsyncF[A, B]
//   (success: => Option[String] = None, failure: => Option[String] = None)
//   (f: Future[B]) = {
//     fabAsync(success, failure)(f.task)
//   }

//   def fabAsync[A, B]
//   (success: => Option[String] = None, failure: => Option[String] = None)
//   (task: Task[B]) = {
//     send(AsyncTask(task, success, failure))
//   }

//   private val fadeTime = 400L

//   lazy val fadeToProgress = (fab <~~ fadeOut(fadeTime) <~ hide) ~
//     (progress <~~ fadeIn(fadeTime) <~ show)

//   lazy val fadeToFab = (progress <~~ fadeOut(fadeTime) <~ hide) ~
//     (fab <~~ fadeIn(fadeTime) <~ show)

//   lazy val headerHeight = res.dimen("header_height").toOption | 140.dp.toFloat

//   val lock = new Object

//   var changingFabVisibility = false

//   var scrollHeight = 0

//   def changeFabVisibility(snail: Snail[View]) {
//     changingFabVisibility = true
//     Ui.run((fab <~~ snail) ~~ Ui {
//       changingFabVisibility = false
//       syncFabVisibility()
//     })
//   }

//   def fabVisible = fab.ui.exists(_.isShown)

//   def showFab() {
//     if(!fabVisible) changeFabVisibility(fadeIn(fadeTime))
//   }

//   def hideFab() {
//     if(fabVisible) changeFabVisibility(fadeOut(fadeTime))
//   }

//   def syncFabVisibility() {
//     lock synchronized {
//       if (!changingFabVisibility)
//         if(scrollHeight < fabHideThresh) showFab() else hideFab()
//     }
//   }

//   def updateFabPosition() {
//     Ui.run(fab <~ translateY(-scrollHeight))
//     syncFabVisibility()
//   }

//   def fabHideThresh = headerHeight / 2

//   def scrolled(view: ViewGroup, height: Int) {
//     scrollHeight = height
//     updateFabPosition()
//     Ui.run(header <~ parallaxScroll(height))
//   }
// }
