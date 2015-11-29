package tryp.droid
package meta

import scala.math.min

import android.widget.{NumberPicker,EditText}
import android.text.InputType
import android.graphics.drawable.Drawable

import tryp.droid.util._

trait AndroidExt
{
  implicit class ViewGroupExt(vg: ViewGroup) {
    def children: Seq[View] = {
      (0 until vg.getChildCount) map { i ⇒ vg.getChildAt(i) }
    }
  }

  implicit class ViewExt(val view: View)
  extends tryp.droid.Basic
  with tryp.droid.Searchable
  {
    def context = view.getContext

    def clickListen(callback: (View) ⇒ Unit) {
      view.setOnClickListener(new android.view.View.OnClickListener {
        def onClick(v: View) = callback(v)
      })
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
      if (TrypEnv.release && interval == "minute") {
        10
      }
      else if (TrypEnv.debug && interval == "second") {
        1
      }
      else {
        0
      }
    }
  }

  implicit class `Fragment extensions`(f: Fragment)
  extends tryp.droid.Basic
  with tryp.droid.Searchable
  with tryp.droid.FragmentManagement
  {
    def view = f.getView
    def activity = f.getActivity
    def getFragmentManager = f.getChildFragmentManager
  }

  implicit class `Activity extensions`(a: Activity)
  extends tryp.droid.Basic
  with tryp.droid.Searchable
  with tryp.droid.FragmentManagement
  {
    override def context = a
    def view = a.getWindow.getDecorView.getRootView
    override def activity = a
    def getFragmentManager = a.getFragmentManager
  }
}
