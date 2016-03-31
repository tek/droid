// package tryp
// package droid

// import view._
// import view.core._
// import state._

// import scalaz._, syntax.show._, syntax.nel._, syntax.traverse._

// abstract class HasActivityAndroid[F[_, _]: ConsIO]
// extends ContextAndroid[F]
// with ResourcesAccess
// with Snackbars
// with Transitions
// with TrypActivityAccess
// with HasSettings
// with Input
// {
//   override def loadFragment(fragment: FragmentBuilder) = {
//     IO {
//       // activity.replaceFragment(
//       //   fragment.id, fragment(), false, fragment.tag, false)
//       "fragment loaded successfully"
//     }
//   }

//   override def transitionFragment(fragment: FragmentBuilder) = {
//     settings.app.bool("view_transitions", true)().fold(trypActivity, None)
//       .map { a =>
//         IO[String] {
//           implicit val fm = FragmentManagement.activityFragmentManagement[Activity]
//           val ui = Macroid.frag(
//             activity, fragment(), fragment.id, fragment.tag)
//           a.transition(ui)
//           "Transition successful"
//         }
//       }
//       .getOrElse(loadFragment(fragment) map(_ => "Cannot transition fragment"))
//   }

//   override def failure[E: Show](e: NonEmptyList[E]) = {
//     Log.d(s"handling failure in activity: $e")
//     snackbarLiteral(e.map(_.show).toList.mkString("\n"))
//   }

//   // override def notify(id: String) = mkToast(id).getOrElse(IO.nop)

//   override def hideKeyboard() = {
//     super[Input].hideKeyboard()
//   }

//   def startActivity(cls: Class[_ <: Activity]): Int = {
//     val intent = new Intent(activity, cls)
//     activity.startActivity(intent)
//   }
// }

// abstract class ActivityAndroid[F[_, _]: ConsIO]
// extends HasActivityAndroid[F]
// {
//   def getFragmentManager = activity.getFragmentManager
// }

// class DefaultActivityAndroid[F[_, _]: ConsIO](implicit val activity: Activity)
// extends ActivityAndroid[F]

// object ActivityAndroid
// {
//   def default[F[_, _]: ConsIO](implicit a: Activity) = 
//     new DefaultActivityAndroid[F]
// }

// abstract class FragmentAndroid[F[_, _]: ConsIO]
// extends HasActivityAndroid[F]
// {
//   val fragment: Fragment

//   def getFragmentManager = fragment.getChildFragmentManager
// }

// class DefaultFragmentAndroid[F[_, _]: ConsIO](implicit val fragment: Fragment)
// extends FragmentAndroid[F]
// {
//   val activity = fragment.activity
// }

// object FragmentAndroid
// {
//   def default[F[_, _]: ConsIO](implicit f: Fragment) = 
//     new DefaultFragmentAndroid[F]
// }

// // class IOOps[A](ui: IO[A])
// // (implicit ctx: AndroidUiContext, ec: EC)
// // {
// //   def attemptIO = {
// //     Log.d(s"running the IO")
// //     IO.run(ui)
// //       .flatMap(handleIOResult)
// //       .andThen { case Failure(e) => uiError(e) }
// //   }

// //   def handleIOResult(a: A) = {
// //     Log.d(s"handling IO result $a")
// //     Future.successful(a)
// //   }

// //   def uiError(e: Throwable) = {
// //     Log.d(s"logging IO error")
// //     ctx.uiError(e)
// //   }
// // }

// // trait ToIOOps
// // {
// //   implicit def ToIOOps[A](a: IO[A])
// //   (implicit ctx: AndroidUiContext, ec: EC, info: DbInfo) = {
// //     new IOOps(a)
// //   }
// // }

// // TODO replace Log with Writer
// // at the end of the universe, send written items to generic log, may be
// // snackbars, stdout or android etc.
// // class IOValidationNelActionOps[E: Show, A](a: IOAction[E, A])
// // (implicit ctx: AndroidUiContext, ec: EC, info: DbInfo)
// // extends ToIOOps
// // {
// //   def attemptIO: Unit = {
// //     Log.d(s"running action")
// //     a.!.task unsafePerformAsync {
// //       case \/-(r) => handleActionResult(r)
// //       case -\/(e) => dbError(e)
// //     }
// //   }

// //   def handleActionResult(result: IOActionResult[E, A]) = {
// //     Log.d(s"handling action result: $result")
// //     result fold(ctx.failure(_), _.attemptIO)
// //   }

// //   def dbError(e: Throwable) = {
// //     Log.d(s"logging db error")
// //     ctx.dbError(e)
// //   }
// // }

// // trait ToIOValidationNelActionOps
// // {
// //   implicit def ToIOValidationNelActionOps[E: Show, A]
// //   (a: IOAction[E, A])
// //   (implicit ec: EC, info: DbInfo, ctx: AndroidUiContext) = {
// //     new IOValidationNelActionOps(a)
// //   }
// // }

