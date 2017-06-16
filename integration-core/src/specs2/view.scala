package tryp
package droid
package integration

import org.specs2.matcher.{Matcher, MatchResult, Expectable, TrypExpectable}

import tryp.unit.Match

trait BiMatch[A, B]
{
  def result(expectable: Expectable[A], target: B): MatchResult[A]
}

class BiMustExpectable[A](e: => A)
extends TrypExpectable[A](() => e)
{
  def must[B](matcher: B)(implicit bm: BiMatch[A, B]): MatchResult[A] =
    bm.result(this, matcher)
}

trait BiMustExpectations
{
  implicit def expectable[A](e: => A): BiMustExpectable[A] =
    new BiMustExpectable(e)
}

class ContainA[B]

case class Contain[B](target: B)

case class Nested[A, B](outer: A, inner: B)

trait ViewMatchers
{
  def containA[B] = new ContainA[B]
  def contain[B](b: B) = new Contain[B](b)

  implicit def BiMatch_RootView_ContainA[A: RootView, B <: View: ClassTag]: BiMatch[A, ContainA[B]] =
    new BiMatch[A, ContainA[B]] {
      def result(e: Expectable[A], matcher: ContainA[B]): MatchResult[A] = {
        val value = e.value
        val v = ToSearchView(value).viewOfType[B]
        val name = className[B]
        Matcher.result(v.isDefined, s"$value contains a $name", s"$value does not contain a $name", e)
      }
    }

  implicit def BiMatch_RootView_Contain[A: RootView, B <: View: ClassTag]: BiMatch[A, Contain[B]] =
    new BiMatch[A, Contain[B]] {
      def result(e: Expectable[A], matcher: Contain[B]): MatchResult[A] = {
        val value = e.value
        val v = ToSearchView(value).viewOfType[B]
        val b = matcher.target
        Matcher.result(v.contains(b), s"$value contains $b", s"$value does not contain $b", e)
      }
    }

  implicit def BiMatch_RootView_Nested[A: RootView, B, C](implicit inner: BiMatch[B, C]): BiMatch[A, Nested[B, C]] =
    new BiMatch[A, Nested[B, C]] {
      def result(e: Expectable[A], matcher: Nested[B, C]): MatchResult[A] = {
        val value = e.value
        Matcher.result(false, "", "", e)
      }
    }
}

object ViewMatch
{
  case class ContainsAView[A]()

  object ContainsAView
  {
    implicit def Match_ContainsAView_mono[A: RootView, B <: View: ClassTag]: Match[A, ContainsAView, B, B] =
      new Match[A, ContainsAView, B, B] {
        def apply(a: A, fb: ContainsAView[B]): Either[String, B] = {
          val name = className[B]
          a.viewOfType[B] match {
            case Some(a) => Right(a)
            case None => Left(s"$a does not contain a $name")
          }
        }
      }
  }
}

trait ViewMatchCons
{
  import ViewMatch.ContainsAView

  def containA[A] = ContainsAView[A]()
}
