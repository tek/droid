package tryp
package droid

import android.widget._
import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.Snails

import akka.actor.ActorSelection
import akka.pattern.ask

import scalaz._, Scalaz._

import tweaks.Slot

case class SimpleModelViewHolder(view: View, root: Slot[RelativeLayout],
  name: Slot[TextView], delete: Slot[ImageButton])
extends RecyclerView.ViewHolder(view)

class SimpleModelAdapter[A <: NamedModel: ClassTag]
(dao: Dao[A])
(implicit activity: Activity, dbi: DbInfo, ctx: AndroidUiContext)
extends SimpleRecyclerAdapter[SimpleModelViewHolder, A]
with Macroid
with DbProfile
with ToUiValidationNelActionOps
{
  fetch.attemptUi

  def fetch = dao.!.vmap(updateItems)

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    val root = slut[RelativeLayout]
    val name = slut[TextView]
    val delete = slut[ImageButton]
    val pad = padding(all = 32 dp)
    val layout = clickFrame(
      RL(↔)(
        w[TextView] <~ name <~ rlp(↤, centerv) <~ pad <~ txt.large,
        w[ImageButton] <~ delete <~ image("ic_delete") <~ rlp(↦, centerv) <~
          pad
      ) <~ root
    )
    SimpleModelViewHolder(Ui.get(layout), root, name, delete)
  }

  def onBindViewHolder(holder: SimpleModelViewHolder, position: Int) {
    items.lift(position) foreach { item ⇒
      Ui.run(
        holder.root <~ On.click { enhance(holder, item) },
        holder.name <~ txt.literal(item.name),
        holder.delete <~ On.click { delete(position, item) }
      )
    }
  }

  def enhance(holder: SimpleModelViewHolder, item: A) = {
    core ! Messages.ShowDetails(item)
    Ui.nop
    // holder.name <~ SharedWidgets.itemName <~~
    //   Snails.wait(core ? Messages.ShowDetails(item))
  }

  def delete(position: Int, item: A) = {
    dao.delete(item).!
    simpleItems.remove(position)
    applyFilter
  }
}
