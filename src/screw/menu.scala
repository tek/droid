package tryp.droid

import android.content.{Context,Intent}
import android.view.MenuItem

import tryp.droid.res._

trait MenuScrews
extends tryp.droid.tweaks.ResourcesAccess
{
  protected case class Menu(implicit c: Context, ns: ResourceNamespace)
  {
    val always = MenuItem.SHOW_AS_ACTION_ALWAYS
    val collapse = MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
    val ifRoom = MenuItem.SHOW_AS_ACTION_IF_ROOM
    val never = MenuItem.SHOW_AS_ACTION_NEVER
    val withText = MenuItem.SHOW_AS_ACTION_WITH_TEXT

    def icon(name: String) = Screw[MenuItem] { _.setIcon(theme.drawable(name)) }

    def show(flags: Int) = Screw[MenuItem] { _.setShowAsAction(flags) }

    def id(i: Id) = Screw[MenuItem] { m ⇒
      val intent = new Intent
      intent.putExtra("menu_id", i.value)
      m.setIntent(intent)
    }

    def click(f: ⇒ Unit) = Screw[MenuItem] {
      _.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener {
        def onMenuItemClick(item: MenuItem) = {
          f
          true
        }
      })
    }
  }

  def mnu(implicit c: Context, ns: ResourceNamespace) = Menu()
}

object MenuScrews
extends MenuScrews
