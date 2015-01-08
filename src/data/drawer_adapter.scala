package tryp.droid

import android.widget._
import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.contrib.TextTweaks._

import akka.actor.ActorSelection

import tryp.droid.Macroid._

case class DrawerViewHolder(view: View, text: Slot[TextView])
extends RecyclerView.ViewHolder(view)

class DrawerAdapter(actor: Option[ActorSelection])
(implicit activity: Activity)
extends SimpleRecyclerAdapter[DrawerViewHolder, String]
{
  setHasStableIds(true)
  updateItems(Seq("Plan", "Shop", "Settings", "Help"))

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    val text = slut[TextView]
    val layout = w[TextView] <~ whore(text) <~ padding(all = 8 dp) <~ medium <~
      selectable <~ ↔
    new DrawerViewHolder(getUi(layout), text)
  }

  def onBindViewHolder(holder: DrawerViewHolder, position: Int) {
    val item = items(position)
    runUi {
      holder.text <~ txt.literal(item) <~ On.click {
        Ui(actor ! Messages.DrawerClick(position))
      }
    }
  }
}
