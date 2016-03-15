package tryp
package droid
package state

import ZS._

import shapeless._
import tag.@@

import ScalazGlobals._

import reflect.macros.blackbox

class Publish
extends annotation.StaticAnnotation
{
  def macroTransform(annottees: Any*): Any = macro PublishMacros.process
}

class PublishMacros(val c: blackbox.Context)
extends Annotation
{
  import c.universe._

  override def setPositions = false

  def filter(tpe: Type) = {
    val name = tpe.typeSymbol.name.toString
    val term = TermName(s"${name}_publishFilter")
    q"""
    implicit val $term = new PublishFilter[$tpe] {
      def allowed = true
    }
    """
  }

  def isMessage(tp: Type) = tp.baseClasses contains(symbolOf[Message])

  def messageType(t: Tree) = {
    val tpe = c.Expr[Any](c.typecheck(t)).actualType
    if (isMessage(tpe)) tpe
    else if (isMessage(tpe.companion)) tpe.companion
    else abort(s"Publish() parameter is not a Message subclass: $t")
  }

  def filterTrans(ann: Annottees) = {
    val imprt = q"import tryp.droid.state.PublishFilter"
    val filters = params map(messageType) map(filter)
    (Annottees.cls ^|-> ClassData.body ^|-> BodyData.misc)
      .prepend(imprt :: filters)(ann)
  }

  def transformers = filterTrans _ :: Nil
}

trait Message
{
  def publish = Parcel(this, true)
  def internal = Parcel(this, false)
}

trait Loggable
extends Message
{
  def message: String
}

case class UnknownResult[A: Show](result: A)
extends Loggable
{
  def message = result.show.toString
}

trait MessageInstances
{
  implicit def messageShow[A <: Message] = Show.shows[A] {
    case res: Loggable => s"${res.className}(${res.message})"
    case a => a.toString
  }

  type MProc = Process[Task, Message]

  implicit lazy val mProcMonoid =
    Monoid.instance[MProc]((a, b) => a.merge(b), Process.halt)

  implicit lazy val mProcMonoidNon: algebra.Monoid[MProc] =
    new algebra.Monoid[MProc] {
      def empty = Process.halt
      def combine(x: MProc, y: MProc) = x.merge(y)
    }
}

case class Parcel(message: Message, publish: Boolean)
{
  def fail = this.failureNel[Parcel]
  def success = this.successNel[Parcel]
}

trait ParcelInstances
{
  implicit val ShowParcel = Show.shows[Parcel](_.message.shows)
}

trait PublishFilter[A <: Message]
{
  def allowed: Boolean
  def parcel(a: A) = Parcel(a, allowed)
}

object Parcel
extends ParcelInstances
{
  // implicit def messageToParcel[A <: Message](message: A): Parcel =
  //   macro ParcelerMacros.messageToParcel[A]

  implicit def messageToParcel[A <: Message](m: A)
  (implicit pf: PublishFilter[A]): Parcel = {
    pf.parcel(m)
  }

  // implicit def messageToParcel[A <: Message](message: A)
  // (implicit pf: PublishFilter[A]): Parcel =
  //   Parcel(message, pf.allowed)
}

// object ParcelerMacros
// {
//   def messageToParcel[A <: Message: c.WeakTypeTag](c: blackbox.Context)
//   (message: c.Expr[A]) = {
//     import c.universe._
//     val tp = weakTypeOf[A]
//     q"""
//     Parcel(
//       $message,
//       implicitly[${typeOf[MessageFilterI @@ Pub]}].allow[$tp]
//     )
//     """
//   }
// }

object Message
extends MessageInstances

class MessageChannel[A]

abstract class AcceptMessages[A]
{
  def channels: List[MessageChannel[_]]

  def filter[B]: Boolean = true
}

trait MessageFilterI
{
  def allow[A]: Boolean = true
}

trait MessageFilter[A]
extends MessageFilterI

trait Pub
trait Sub

object MessageFilter
{
  def pub[A <: HList]: MessageFilter[A] = macro MessageFilterMacros.pub[A]

  implicit def materializeMessageFilter[A <: HList]: MessageFilter[A] @@ Pub =
    macro MessageFilterMacros.pub[A]
}

// TODO
// make implicit parameter containing the publish flag a type class for the
// message type, define implicit values manually in Machine subclasses
// fuck this shit
class MessageFilterMacros(val c: blackbox.Context)
extends MacroMetadata
{
  import c.universe._

  def hlistTypes(tp: Type): List[Type] = {
    tp match {
      case a if a =:= typeOf[HNil] =>
        Nil
      case a if a <:< typeOf[HList] =>
        a.typeArgs match {
          case List(h, t) => h :: hlistTypes(t)
          case t =>
            abort(s"invalid type params for MessageFilter: $t ($tp)")
        }
      case _ =>
        abort(s"MessageFilter params must be HList, got $tp")
    }
  }

  def pub[A <: HList: c.WeakTypeTag] = {
    val tp = weakTypeOf[A]
    val anyVerdict = q"""
    implicit def default[A] = at[A](_ => false)
    """
    val verdicts = hlistTypes(tp) map { t =>
      val name = TermName(s"${t.typeSymbol.name.d}")
      q"""
      implicit def $name = at[$t](_ => true)
      """
    }
    q"""
    class MF
    extends tryp.droid.state.MessageFilter[$tp]
    shapeless.tag.apply[${typeOf[Pub]}](new MF)
    """
  }

  def allow[A: c.WeakTypeTag, B <: HList: c.WeakTypeTag] = {
    val verdict = hlistTypes(weakTypeOf[B]) contains(weakTypeOf[A])
    q"$verdict"
  }
}
