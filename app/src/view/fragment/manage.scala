package tryp
package droid

import reflect.classTag

import android.app.{FragmentManager, FragmentTransaction}

import ScalazGlobals._

import simulacrum._

import view.{HasActivityF, HasContextF}

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

@typeclass abstract class FragmentManagement[A]
(implicit val hasActivity: HasActivityF[A], val hasContext: HasContextF[A],
  val rv: RootView[A])
extends FragmentHelpers
{
  def childFragmentManager(a: A): FragmentManager

  def rootFragmentManager(a: A) = a.activity.getFragmentManager

  def checkFrame[B: ResId](a: A)(name: B, check: Boolean = true)
  (f: => Unit)
  {
    if (!check || a.viewExists(name))
      f
    else
      Log.e(s"Tried to add fragment to nonexistent frame with id '$name'")
  }

  def findFragment(a: A)(tag: String) = {
    Option[Fragment](rootFragmentManager(a).findFragmentByTag(tag))
  }

  def replaceFragment[B: ResId](a: A)(name: B, fragment: Fragment,
    backStack: Boolean, tag: String, check: Boolean = true)
    {
      a.res.id(name) foreach { id =>
        moveFragment(a)(name, fragment, backStack, tag, check) {
          _.replace(id, fragment, tag)
        }
      }
    }

  // Check for existence of 'fragment' by 'tag', insert the new one if not
  // found
  // Return true if the fragment has been inserted
  // TODO allow overriding the check for existence for back stack fragments
  def replaceFragmentIf(a: A)(name: RId, fragment: => Fragment,
    backStack: Boolean, tag: String) =
  {
    val frag = findFragment(a)(tag)
    frag ifNone { replaceFragment(a)(name, fragment, backStack, tag) }
    frag isEmpty
  }

  def replaceFragmentCustom(a: A)
  (id: RId, fragment: Fragment, backStack: Boolean) =
  {
    replaceFragmentIf(a)(id, fragment, backStack,
      fragmentClassName(fragment.getClass))
  }

  def replaceFragmentAuto[B <: Fragment: ClassTag](a: A)(id: RId,
    backStack: Boolean) =
  {
    val tag = Tag(fragmentName[B])
    replaceFragmentIf(a)(id, makeFragment[B], backStack, tag)
  }

  def clearBackStack(a: A)() = {
    backStackNonEmpty(a) tapIf {
        childFragmentManager(a).popBackStack(null,
          FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
  }

  def addFragment[B: ResId](a: A)(name: B, fragment: Fragment,
    backStack: Boolean, tag: String, check: Boolean = true)
  {
      a.res.id(name) foreach { id =>
        moveFragment(a)(name, fragment, backStack, tag, check) {
          _.add(id, fragment, tag)
        }
      }
  }

  def moveFragment[B: ResId](a: A)(name: B, fragment: Fragment,
    backStack: Boolean, tag: String, check: Boolean = true)
  (move: FragmentTransaction => Unit)
  {
    checkFrame(a)(name, check) {
      val trans = childFragmentManager(a).beginTransaction
      move(trans)
      if (backStack) {
        trans.addToBackStack(tag)
      }
      trans.commit
    }
  }

  def addFragmentUnchecked[B <: Fragment: ClassTag](a: A)(ctor: => B) {
    val name = fragmentName[B]
    addFragment(a)(RId(name), ctor, false, Tag(name), false)
  }

  def addFragmentIf[B <: Fragment: ClassTag](a: A)(ctor: => B) {
    val name = fragmentName[B]
    if (!fragmentExists[B](a))
      replaceFragment(a)(RId(name), ctor, false, Tag(name))
  }

  def fragmentExists[B <: Fragment: ClassTag](a: A) = {
    val name = fragmentName[B]
    val tag = Tag(name)
    findFragment(a)(tag) isDefined
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
