package tryp.droid

import android.widget._
import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.contrib.TextTweaks._

import akka.actor.ActorSelection

import tryp.droid.Macroid._
import tryp.droid.res.PrefixResourceNamespace

case class DrawerViewHolder(view: View, text: Slot[TextView])
extends RecyclerView.ViewHolder(view)

class DrawerAdapter(actor: Option[ActorSelection], navigation: Navigation)
(implicit activity: Activity)
extends SimpleRecyclerAdapter[DrawerViewHolder, NavigationTarget]
{
  setHasStableIds(true)
  updateItems(navigation.drawerItems)

  implicit val ns = PrefixResourceNamespace("drawer")

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    val text = slut[TextView]
    val layout = w[TextView] <~ whore(text) <~ padding(all = 8 dp) <~ medium <~
      selectable <~ ↔
    new DrawerViewHolder(getUi(layout), text)
  }

  def onBindViewHolder(holder: DrawerViewHolder, position: Int) {
    val item = items(position)
    val color = bgCol(navigation.isCurrent(item) ? "item_selected" / "item")
    runUi {
      holder.text <~ txt.literal(item.title) <~ On.click {
        Ui(actor ! Messages.Navigation(item))
      }
    }
  }
}
