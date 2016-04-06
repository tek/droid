package tryp
package droid
package view

import simulacrum._

import scalaz._, Scalaz._

@typeclass trait RootView[A]
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
{
  def searcher: SearchView.CanFindView = a.root

  protected def invalidId(id: String)(implicit res: Resources) = {
    if (TrypEnv.release)
      Xor.left[Throwable, View](new Exception(s"invalid view id: $id"))
    else {
      val msg = s"Couldn't find a view with id '$id'! " +
      s"Current views: ${RId.ids}\n" +
      s"tree:\n" + viewTree.drawTree
      throw new ClassCastException(msg)
    }
  }

  def findId(id: Int)(implicit res: Resources): Xor[Throwable, View] = {
    Option(searcher.findViewById(id)) match {
      case Some(v) => Xor.right[Throwable, View](v)
      case _ => invalidId(id.toString)
    }
  }

  def viewsOfType[B <: View: ClassTag]: Seq[B] = {
    a.root match {
      case v: B => Seq(v)
      case layout: ViewGroup =>
        layout.children map {
          case v: B => Seq(v)
          case sub: ViewGroup => sub.viewsOfType[B]
          case _ => Nil
        } flatten
      case _ => Nil
    }
  }

  def viewOfType[B <: View: ClassTag] = {
    viewsOfType[B].headOption
  }

  def viewTree: Tree[View] = {
    a.root match {
      case vg: ViewGroup =>
        (vg: View).node(vg.children map(_.viewTree): _*)
      case v =>
        v.leaf
    }
  }

  def showViewTree = viewTree.drawTree

  def printViewTree() = Log.i(showViewTree)
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
