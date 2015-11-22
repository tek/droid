package tryp
package droid
package test

class IOBSpec
extends SpecBase
{
  def is = s2"""
  signal $signal
  """

  def before = {
    activity
  }

  def signal = {
    1 === 1
  }
}
