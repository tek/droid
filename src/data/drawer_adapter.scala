package tryp.droid

import android.widget._
import android.support.v7.widget.RecyclerView
import android.graphics.{Color,PorterDuff}

import macroid.FullDsl._
import macroid.contrib.TextTweaks.{medium, bold}

import akka.actor.ActorSelection

import com.makeramen.roundedimageview.RoundedImageView

import tryp.droid.Macroid._
import tryp.droid.res.PrefixResourceNamespace

sealed abstract class DrawerViewHolder(v: View)
extends RecyclerView.ViewHolder(v)
{
  def view: View

  def text: Slot[TextView]
}

case class NavViewHolder(view: View, text: Slot[TextView])
extends DrawerViewHolder(view)

case class GPlusHeaderHolder(view: View, name: Slot[TextView],
  email: Slot[TextView], avatar: Slot[RoundedImageView])
extends DrawerViewHolder(view)
{
  def text = name
}

class DrawerAdapter(navigation: Navigation)
(implicit activity: Activity)
extends SimpleRecyclerAdapter[DrawerViewHolder, DrawerItem]
{
  setHasStableIds(true)
  updateItems(navigation.drawerItems)

  implicit val ns = PrefixResourceNamespace("drawer")

  override def getItemViewType(position: Int) = {
    items(position) match {
      case t: NavigationTarget ⇒ 0
      case h: GPlusHeader ⇒ 1
    }
  }

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    viewType match {
      case 1 ⇒ gPlusHolder
      case _ ⇒ navHolder
    }
  }

  def navHolder = {
    val text = slut[TextView]
    val layout = clickFrame(
      w[TextView] <~ whore(text) <~ padding(all = 16 dp) <~ medium <~ ↔
    ) <~ ↔
    NavViewHolder(getUi(layout), text)
  }

  def gPlusHolder = {
    val name = slut[TextView]
    val email = slut[TextView]
    val avatar = slut[RoundedImageView]
    val photoSize = 64 dp
    val textTweaks = txt.ellipsize() + txt.color("header_text")
    val layout = RL(flp(↔, Height(res.d("header_height"))))(
      w[TextView] <~ whore(name) <~ textTweaks <~ rlp(↤, above(Id.email)) <~
        bold <~ margin(left = 8 dp, top = 16 dp),
      w[TextView] <~ whore(email) <~ textTweaks <~ Id.email <~
        rlp(↤, ↧) <~ margin(left = 8 dp, bottom = 8 dp),
      w[RoundedImageView] <~ whore(avatar) <~
        imageCornerRadius(photoSize / 2) <~ imageBorderWidth(2) <~
        imageBorderColor("header_photo") <~
        rlp(Width(photoSize), Height(photoSize), ↥, ↤) <~ margin(all = 16 dp)
    )
    GPlusHeaderHolder(getUi(layout), name, email, avatar)
  }

  def onBindViewHolder(holder: DrawerViewHolder, position: Int) {
    items(position) match {
      case t: NavigationTarget ⇒ bindNavTarget(holder, t)
      case h: GPlusHeader ⇒ bindGPlusHeader(holder, h)
    }
  }

  def bindNavTarget(holder: DrawerViewHolder, target: NavigationTarget) = {
    val color = bgCol(navigation.isCurrent(target) ? "item_selected" | "item")
    runUi(
      holder.text <~ txt.literal(target.title),
      holder.view <~ color <~ On.click {
        Ui(core ! Messages.Navigation(target))
      }
    )
  }

  def bindGPlusHeader(holder: DrawerViewHolder, header: GPlusHeader) = {
    holder match {
      case GPlusHeaderHolder(view, name, email, avatar) ⇒
        GPlus { account ⇒
          account.withCover { cover ⇒
            cover.setColorFilter(Color.argb(80, 0, 0, 0),
              PorterDuff.Mode.DARKEN)
            runUi(view <~ bg(cover))
          }
          account.withPhoto { photo ⇒
            runUi(avatar <~ imageDrawableC(photo))
          }
          runUi(
            name <~ account.name.map { n ⇒ txt.literal(n) },
            email <~ account.email.map { n ⇒ txt.literal(n) }
          )
        }
      case _ ⇒ throw new java.lang.RuntimeException(
          s"Invalid view holder for GPlusHeader: ${holder.className}")
    }
  }
}
