package tryp.droid

import scala.math.min

import android.widget.{NumberPicker,EditText}
import android.text.InputType
import android.graphics.drawable.Drawable

import tryp.droid.util.{Params,OS}
import tryp.droid.view.{ViewsProxy,TypedViewsProxy}

object AndroidExt {
  implicit class ViewGroupExt(vg: ViewGroup) {
    def children: List[View] = {
      val ret = for (i <- 0 until vg.getChildCount) yield { vg.getChildAt(i) }
      ret.toList
    }
  }

  implicit class ViewExt(val view: View)
  extends tryp.droid.Basic
  with tryp.droid.view.Searchable
  {
    def context = view.getContext

    def clickListen(callback: (View) ⇒ Unit) {
      view.setOnClickListener(new android.view.View.OnClickListener {
        def onClick(v: View) = callback(v)
      })
    }

    def params: Params = view.getTag match {
      case p: Params ⇒ p
      case m: Map[_, _] ⇒ Params.fromErasedMap(m)
      case _ ⇒ Params()
    }

    def setBackgroundCompat(drawable: Drawable) {
      if (OS.hasViewSetBackground) {
        view.setBackground(drawable)
      }
      else {
        view.setBackgroundDrawable(drawable)
      }
    }
  }

  // TODO subclass NumberPicker
  // search for preselect value in current value list
  implicit class NumberPickerExt(picker: NumberPicker) {
    def expand = resize(picker.getDisplayedValues.size - 1)

    def truncate = resize(0)

    def resize(index: Int) {
      picker.setMaxValue(index)
      picker.setValue(index)
    }

    def applyValues(values: Array[String]) {
      resize(values.size - 1)
      picker.setDisplayedValues(values)
      expand
    }

    def reset(values: Array[String], interval: String) {
      truncate
      val visible = values.size > 1
      picker.setVisibility(visibility(visible))
      if (visible) {
        picker.applyValues(values)
        preselect(interval)
        edit foreach {
          _.setInputType(InputType.TYPE_CLASS_NUMBER)
        }
      }
    }

    def visibility(state: Boolean) = {
      if (state) android.view.View.VISIBLE else android.view.View.INVISIBLE
    }

    def edit: Option[EditText] = {
      picker.children.find(_.isInstanceOf[EditText]) map { v ⇒
        v match {
          case edit: EditText ⇒ edit
          case _ ⇒ null
        }
      }
    }

    def preselect(interval: String) {
      picker.setValue(
        picker.getValue - min(preselectValue(interval), picker.getMaxValue)
      )
    }

    def preselectValue(interval: String) = {
      if (Env.release && interval == "minute") {
        10
      }
      else if (Env.debug && interval == "second") {
        1
      }
      else {
        0
      }
    }
  }

  implicit class `Fragment extensions`(f: Fragment)
  extends tryp.droid.Basic
  with tryp.droid.view.Searchable
  with tryp.droid.view.Fragments
  {
    def view = f.getView
    def activity = f.getActivity
    def getFragmentManager = f.getChildFragmentManager
  }

  implicit class `Activity extensions`(a: Activity)
  extends tryp.droid.Basic
  with tryp.droid.view.Searchable
  {
    def context = a
    def view = a.getWindow.getDecorView.getRootView
    def activity = a
  }
}
