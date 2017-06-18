package tryp
package droid
package view

import tryp.core.RoseTree

@tc
trait RootView[A]
{
  def root(a: A): View
}

object RootView
{
  implicit def activityRootView[A <: Activity] = new RootView[A] {
    def root(a: A) = a.getWindow.getDecorView.getRootView
  }

  implicit def fragmentRootView[A <: Fragment] = new RootView[A] {
    def root(a: A) = a.getView
  }

  implicit def viewRootView[A <: View] = new RootView[A] {
    def root(a: A) = a
  }
}

final class SearchView[A: RootView](a: A)
extends ViewInstances
with Logging
{
  def searcher: SearchView.CanFindView = a.root

  def viewsOfType[B <: View: ClassTag]: Vector[B] = {
    a.root match {
      case v: B => Vector(v)
      case layout: ViewGroup =>
        layout.children.flatMap {
          case v: B => Vector(v)
          case sub: ViewGroup => sub.viewsOfType[B]
          case _ => Vector.empty
        }
      case _ => Vector.empty
    }
  }

  def viewOfType[B <: View: ClassTag] = viewsOfType[B].headOption

  def viewTree: RoseTree[View] = {
    a.root match {
      case vg: ViewGroup =>
        RoseTree.Node(vg, vg.children.map(_.viewTree))
      case v =>
        RoseTree.Leaf(v)
    }
  }

  def showViewTree = viewTree.draw

  def treeLines = showViewTree.lines

  def showTree = treeLines.dbgLines
}

trait ToSearchView
{
  implicit def ToSearchView[A: RootView](a: A): SearchView[A] =
    new SearchView(a)
}

object SearchView
extends ToSearchView
{
  type CanFindView = AnyRef { def findViewById(id: Int): View }
}
