// package tryp.droid

// import android.widget._
// import android.support.v7.widget.RecyclerView

// import macroid.FullDsl._
// import macroid.contrib.TextTweaks._

// import tryp.droid.tweaks.Recycler._
// import tryp.droid._
// import tryp.droid.Macroid._
// import tryp.droid.meta.InternalLog

// case class LogViewHolder(view: View, text: Slot[TextView])
// extends RecyclerView.ViewHolder(view)

// class LogAdapter(implicit activity: Activity)
// extends SimpleRecyclerAdapter[LogViewHolder, String]
// {
//   setHasStableIds(true)

//   def onCreateViewHolder(parent: ViewGroup, viewType: Int) = {
//     val text = slut[TextView]
//     val layout = w[TextView] <~ whore(text) <~ padding(all = 8 dp) <~ ↔
//     new LogViewHolder(Ui.get(layout), text)
//   }

//   def onBindViewHolder(holder: LogViewHolder, position: Int) {
//     val item = items(position)
//     Ui.run(holder.text <~ txt.literal(item))
//   }
// }

// case class LogFragment()
// extends MainFragment
// with RecyclerFragment[LogAdapter]
// {
//   override def handle = "log"

//   override val actors = Seq(LogActor.props)

//   lazy val adapter = new LogAdapter

//   override def onStart() {
//     super.onStart
//     InternalLog.actor = Some(selectActor("Log"))
//     Ui.run(updateLog())
//   }

//   def updateLog() = {
//     adapter.updateItems(InternalLog.buffer)
//     Thread.sleep(100)
//     recyclerView <~ scrollTop
//   }

//   def recyclerTweaks = linear + divider + reverseLayout
// }
