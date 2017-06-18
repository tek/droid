package tryp
package droid
package view

import reflect.classTag

import android.app.{FragmentManager, FragmentTransaction}

import HasContext.ops._

trait FragmentHelpers
{
  def fragmentClassName(cls: Class[_]) = {
    cls.className.stripSuffix("Fragment")
  }

  def fragmentName[B <: Fragment: ClassTag] = {
    fragmentClassName(classTag[B].runtimeClass)
  }

  def makeFragment[B <: Fragment: ClassTag] = {
    val cls = classTag[B].runtimeClass
    cls.newInstance.asInstanceOf[Fragment]
  }
}

@tc abstract class FragmentManagement[A: HasContext: HasActivity: RootView]
extends FragmentHelpers
with Logging
{
  def childFragmentManager(a: A): FragmentManager

  def rootFragmentManager(a: A) = a.activity.getFragmentManager

  def findFragment(a: A)(tag: String) = {
    Option[Fragment](rootFragmentManager(a).findFragmentByTag(tag))
  }

  def replaceFragment[B: ResId](a: A)(name: B, fragment: Fragment, backStack: Boolean, tag: String) = {
    a.res.id(name) foreach { id =>
      moveFragment(a)(name, fragment, backStack, tag) {
        _.replace(id, fragment, tag)
      }
    }
  }

  def addFragment[B: ResId](a: A)(name: B, fragment: Fragment, backStack: Boolean, tag: String) =
  {
    a.res.id(name) foreach { id =>
      moveFragment(a)(name, fragment, backStack, tag)(_.add(id, fragment, tag))
    }
  }

  def moveFragment[B: ResId](a: A)(name: B, fragment: Fragment, backStack: Boolean, tag: String)
  (move: FragmentTransaction => Unit)
  = {
    val trans = childFragmentManager(a).beginTransaction
    move(trans)
    if (backStack) trans.addToBackStack(tag)
    trans.commit
  }

  def addFragmentIf[B <: Fragment: ClassTag](a: A)(ctor: => B) {
    val name = fragmentName[B]
    if (!fragmentExists[B](a))
      replaceFragment(a)(RId(name), ctor, false, Tag(name))
  }

  def fragmentExists[B <: Fragment: ClassTag](a: A) = {
    val name = fragmentName[B]
    val tag = Tag(name)
    findFragment(a)(tag).isDefined
  }

  def addFragmentIfAuto[B <: Fragment: ClassTag](a: A) {
    addFragmentIf(a)(makeFragment[B])
  }

  def popBackStackSync(a: A)() = {
    rootFragmentManager(a).popBackStackImmediate()
  }

  def findNestedFrag[B <: Fragment: ClassTag](a: A)
  (tags: Seq[String]): Option[B] =
  {
    import FragmentManagement.ops._
    tags
      .lift(0)
      .flatMap(findFragment(a))
      .flatMap { frag =>
      frag.findNestedFrag(tags.tail) orElse {
        frag match {
          case f: B => Some(f)
          case _ => None
        }
      }
    }
  }

  def backStackEmpty(a: A) =
    childFragmentManager(a).getBackStackEntryCount == 0

  def backStackNonEmpty(a: A) = !backStackEmpty(a)
}

object FragmentManagement
{
  implicit def fragmentFragmentManagement[A <: Fragment]
  : FragmentManagement[A] =
    new FragmentManagement[A] {
      def childFragmentManager(a: A) = a.getChildFragmentManager
    }

  implicit def activityFragmentManagement[A <: Activity]
  : FragmentManagement[A] =
    new FragmentManagement[A] {
      def childFragmentManager(a: A) = a.getFragmentManager
    }
}
