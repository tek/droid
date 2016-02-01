package tryp
package droid

import language.dynamics

import android.widget.TextView

import simulacrum._

import cats.data.Xor

import scalaz.syntax.std.option._

trait ProxyBase {
  def extractView(args: Any*): Option[View] = {
    args.lift(0) flatMap {
      _ match {
        case v: View ⇒ Some(v)
        case _ ⇒ None
      }
    }
  }
}

final class Searchable[A: RootView: HasContextF](a: A)
extends ToSearchView
{
  implicit def resources = a.res

  type TypedResult[B <: View] = Throwable Xor B

  type SearchResult = TypedResult[View]

  private[this] def resolve[B: ResId](name: B) = {
    a.res.id(name).map(_.value)
  }

  def find[B: ResId](name: B): SearchResult = {
    resolve(name).flatMap(a.findId)
  }

  def findAt[B: ResId](root: View)(name: B): SearchResult = {
    resolve(name).flatMap(root.findId)
  }

  def findtAt[B: ResId, C <: View: ClassTag](root: View)
  (name: B): TypedResult[C] = {
    findAt(root)(name) match {
      case Xor.Right(c: C) ⇒
        Xor.Right(c)
      case Xor.Right(a) ⇒
        val tpe = className[C]
        Xor.Left(
          new Throwable(s"couldn't cast view $a to specified type $tpe"))
        case Xor.Left(t) ⇒ Xor.Left(t)
    }
  }

  def findt[B: ResId, C <: View: ClassTag](name: B): TypedResult[C] = {
    findtAt(a.root)(name)
  }

  def viewExists[B: ResId](name: B) = {
    Try(find(name)).isSuccess
  }

  def textView[B: ResId]
  (name: B, root: Option[View] = None): TypedResult[TextView] = {
    // implicit val c = context(a)
    val finder = root.map(b ⇒ findtAt[B, TextView](b) _)
      .getOrElse(findt[B, TextView] _)
    finder(name)
  }

  implicit def sf = this

  def tviews = TypedViewsProxy()

  def views = ViewsProxy()

  case class ViewsProxy()
  extends Dynamic
  with ProxyBase
  {
    def applyDynamic(name: String)(args: Any*): SearchResult = {
      val finder = extractView(args).map(b ⇒ findAt[String](b) _) |
        find[String] _
      finder(name)
    }

    def selectDynamic(name: String) = {
      find[String](name)
    }
  }

  case class TypedViewsProxy()
  extends Dynamic
  with ProxyBase
  {
    def applyDynamic[B <: View: ClassTag](name: String)
    (args: Any*): TypedResult[B] = {
      val finder = extractView(args).map(b ⇒ findtAt[String, B](b) _) |
        findt[String, B] _
      finder(name)
    }

    def selectDynamic[B <: View: ClassTag](name: String) = {
      findt[String, B](name)
    }
  }
}

trait ToSearchable
{
  implicit def ToSearchable[A: RootView: HasContextF](a: A) =
    new Searchable(a)
}

object Searchable
extends ToSearchable
{
}
