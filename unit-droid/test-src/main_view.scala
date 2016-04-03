package tryp
package droid
package unit

import android.widget.EditText

import state.MainViewMessages.LoadUi

class MainViewSpec
extends StateAppSpec
{
  def is = s2"""
  load a different main view $loadUi
  """

  override def before = stateApp.publishOne(LoadUi(new Agent3))

  def loadUi = activity willContain view[EditText]
}
