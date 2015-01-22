package tryp.droid

import android.widget._
import android.support.v7.widget.RecyclerView

import macroid.FullDsl._
import macroid.contrib.TextTweaks._

import tryp.droid.tweaks.Recycler._
import tryp.droid.res._
import tryp.droid.Macroid._
import tryp.droid.meta.DebugLog

case class LogViewHolder(view: View, text: Slot[TextView])
extends RecyclerView.ViewHolder(view)

class LogAdapter(implicit activity: Activity)
extends SimpleRecyclerAdapter[LogViewHolder, String]
{
  setHasStableIds(true)

  def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
    val text = slut[TextView]
    val layout = w[TextView] <~ whore(text) <~ padding(all = 8 dp) <~ ↔
    new LogViewHolder(getUi(layout), text)
  }

  def onBindViewHolder(holder: LogViewHolder, position: Int) {
    val item = items(position)
    runUi (
      holder.text <~ txt.literal(item)
    )
  }
}

case class LogFragment()
extends MainFragment
{
  override val actors = Seq(LogActor.props)

  lazy val logAdapter = new LogAdapter

  override def onStart() {
    super.onStart
    DebugLog.actor = selectActor("Log")
    update()
  }

  def update() {
    logAdapter.updateItems(DebugLog.buffer)
  }

  override def macroidLayout(state: Bundle) = {
    FL()(
      w[RecyclerView] <~ recyclerAdapter(logAdapter) <~ linear <~ divider
    )
  }
}
