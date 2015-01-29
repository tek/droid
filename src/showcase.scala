package tryp.droid

import com.github.amlcurran.showcaseview._
import com.github.amlcurran.showcaseview.targets.ViewTarget

import tryp.droid._
import res._

abstract class ShowcaseSet(prefix: String)
(implicit ns: ResourceNamespace, activity: Activity)
extends Basic
{
  implicit val context = activity

  trait Showcase
  {
    implicit val ns = PrefixResourceNamespace(
        s"${prefix}_${this.className.snakeCase}")

    def run(callback: ⇒ Unit) {
      target map { t ⇒ runShowcase(t, callback) } getOrElse callback
    }

    def target: Option[View]

    def runShowcase(target: View, callback: ⇒ Unit) = {
      Try {
        new ShowcaseView.Builder(activity, true)
          .setTarget(new ViewTarget(target))
          .setContentTitle(res.s("showcase_title"))
          .setContentText(res.s("showcase_text"))
          .setShowcaseEventListener(listener(callback))
          .setTextPositioning(ShowcaseView.TextPositioningMode.ABOVE_OR_BELOW)
          .build()
      } recover {
        case InvalidResource(msg) ⇒ Log.e(msg)
        case e ⇒ throw e
      }
    }

    def listener(callback: ⇒ Unit) = new OnShowcaseEventListener {
      def onShowcaseViewHide(showcaseView: ShowcaseView) {}
      def onShowcaseViewShow(showcaseView: ShowcaseView) {}
      def onShowcaseViewDidHide(showcaseView: ShowcaseView) { callback }
      }
  }

  def cases: Seq[Showcase]

  def runAll() = {
    recurse(cases)
    true
  }

  def recurse(remaining: Seq[Showcase]) {
    remaining.headOption foreach { _.run { recurse(remaining.tail) } }
  }
}

trait Showcases
extends HasActivity
with AppPreferences
{
  def checkShowcase() {
    if (!(blockTutorial || alreadyLearned)) {
      val success = showcase()
      appPrefs.set(prefName, success)
    }
  }

  def showcase() = false

  def name: String

  private def prefName = s"learned_${name.toLowerCase}"

  private def blockTutorial = appPrefs.bool("block_tutorial", false)()

  private def alreadyLearned() = appPrefs.bool(prefName, false)()
}
