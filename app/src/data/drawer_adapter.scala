// package tryp
// package droid

// import scalaz._, Scalaz._

// import android.widget._
// import android.support.v7.widget.RecyclerView
// import android.graphics.{Color,PorterDuff}

// import macroid._
// import FullDsl._
// import macroid.contrib.TextTweaks.{medium, bold}

// import akka.actor.ActorSelection

// import com.makeramen.roundedimageview.RoundedImageView

// import core._
// import tweaks.Slot

// sealed abstract class DrawerViewHolder(v: View)
// extends RecyclerView.ViewHolder(v)
// {
//   def view: View

//   def text: Slot[TextView]
// }

// case class NavViewHolder(view: View, text: Slot[TextView])
// extends DrawerViewHolder(view)

// case class GPlusHeaderHolder(view: View, name: Slot[TextView],
//   email: Slot[TextView], avatar: Slot[RoundedImageView])
// extends DrawerViewHolder(view)
// {
//   def text = name
// }

// class DrawerAdapter(navigation: Navigation)
// (implicit activity: Activity, plus: PlusInterface)
// extends SimpleRecyclerAdapter[DrawerViewHolder, DrawerItem]
// with Macroid
// {
//   setHasStableIds(true)
//   updateItems(navigation.drawerItems.toList).run

//   implicit val ns = PrefixResourceNamespace("drawer")

//   override def getItemViewType(position: Int) = {
//     items(position) match {
//       case t: NavigationTarget => 0
//       case h: GPlusHeader => 1
//       case b: DrawerButton => 2
//     }
//   }

//   def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
//     viewType match {
//       case 1 => gPlusHolder
//       case _ => navHolder
//     }
//   }

//   def navHolder = {
//     val text = slut[TextView]
//     val layout = clickFrame(
//       w[TextView] <~ whore(text) <~ padding(all = 16 dp) <~ medium <~ ↔
//     ) <~ flp(↔, ↕)
//     NavViewHolder(Ui.get(layout), text)
//   }

//   def gPlusHolder = {
//     val name = slut[TextView]
//     val email = slut[TextView]
//     val avatar = slut[RoundedImageView]
//     val photoSize = 64 dp
//     val textTweaks = txt.ellipsize() + txt.color("header_text")
//     val height = Height(res.d("header_height").getOrElse(0.0f))
//     val layout = RL(flp(↔, height))(
//       w[TextView] <~ whore(name) <~ textTweaks <~ rlp(↤, above(RId.email)) <~
//         bold <~ margin(left = 8 dp, top = 16 dp),
//       w[TextView] <~ whore(email) <~ textTweaks <~ RId.email <~
//         rlp(↤, ↧) <~ margin(left = 8 dp, bottom = 8 dp),
//       w[RoundedImageView] <~ whore(avatar) <~
//         imageCornerRadius(photoSize / 2) <~ imageBorderWidth(2) <~
//         imageBorderColor("header_photo") <~
//         rlp(Width(photoSize), Height(photoSize), ↥, ↤) <~ margin(all = 16 dp)
//     )
//     GPlusHeaderHolder(Ui.get(layout), name, email, avatar)
//   }

//   def onBindViewHolder(holder: DrawerViewHolder, position: Int) {
//     items(position) match {
//       case t: NavigationTarget => bindNavTarget(holder, t)
//       case h: GPlusHeader => bindGPlusHeader(holder, h)
//       case b: DrawerButton => bindButton(holder, b)
//     }
//   }

//   def bindNavTarget(holder: DrawerViewHolder, target: NavigationTarget) = {
//     val ct = implicitly[macroid.CanTweak[View, Tweak[View], View]]
//     val color = bgCol(navigation.isCurrent(target) ? "item_selected" | "item")
//     Ui.run(
//       holder.text <~ txt.literal(target.title),
//       holder.view <~ color <~ On.click {
//         Ui(core ! Messages.Navigation(target))
//       }
//     )
//   }

//   def bindGPlusHeader(holder: DrawerViewHolder, header: GPlusHeader) = {
//     holder match {
//       case GPlusHeaderHolder(view, name, email, avatar) =>
//         plus.oneAccount
//           .map { account =>
//             val cover = account.coverDrawable.infraRun("set plus cover").join
//               .map { cover =>
//                 cover.setColorFilter(Color.argb(80, 0, 0, 0),
//                   PorterDuff.Mode.DARKEN)
//                 view <~ bg(cover)
//               }
//             val photo = account.photoDrawable.infraRun("set plus photo").join
//               .map { photo =>
//                 avatar <~ imageDrawableC(photo)
//               }
//             val actions = List(
//               name <~ account.name.map { n => txt.literal(n) },
//               email <~ account.email.map { n => txt.literal(n) }
//             ) ++ cover.toList ++ photo.toList
//             Ui.sequence(actions: _*).run
//           }
//           .runLog
//           .unsafePerformAsync {
//             case -\/(err) => log.error(err)("bindGPlusHeader")
//             case _ =>
//           }
//       case _ =>
//         sys.error(s"Invalid view holder for GPlusHeader: ${holder.className}")
//     }
//   }

//   def bindButton(holder: DrawerViewHolder, button: DrawerButton) = {
//     val color = bgCol("item")
//     Ui.run(
//       holder.text <~ txt.literal(button.title),
//       holder.view <~ color <~ On.click {
//         Ui(core ! Messages.DrawerClick(button.action))
//       }
//     )
//   }
// }
