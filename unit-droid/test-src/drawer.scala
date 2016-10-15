package tryp
package droid
package unit

import android.support.v4.widget.DrawerLayout

import state.{DrawerAgent, DrawerMachine, MainViewMachine, ToolbarMachine}

object DrawerSpec
{
  def items = List("first", "second")

  import IOOperation.exports._

  class SpecAgent
  extends DrawerAgent

  class SpecAgents
  extends ActivityAgent
  {
    lazy val drawerMachine =
      new DrawerMachine {
        override def handle = "spec"

        def contentLayout = mainViewMachine.layout

        def toolbar = toolbarMachine.toolbar

        def admit: Admission = PartialFunction.empty
      }

    lazy val mainViewMachine = new MainViewMachine {}

    lazy val toolbarMachine: ToolbarMachine = new ToolbarMachine {
      def belowToolbarLayout = drawerMachine.layout
    }

    def viewMachine = toolbarMachine

    override def machines =
      mainViewMachine :: drawerMachine :: super.machines
  }
}

class DrawerSpec
extends StateAppSpec
{
  import DrawerSpec._

  def is = s2"""
  run $run
  """

  override lazy val initialAgent = new SpecAgent

  def run = {
    activity willContain view[Toolbar] and {
      activity.printViewTree()
    activity willContain view[DrawerLayout] }
  }
}
