package slickmacros

import scala.reflect.macros.whitebox.Context
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{ universe => u }
import scala.language.experimental.macros
import scala.reflect.api._

import scala.language.dynamics
import scala.language.higherKinds
import scala.language.postfixOps
import scala.slick.lifted.{ Query => LQuery }
import scala.slick.lifted.Rep

/**
 * Work in progress
 * will update timestamp on insert & updates
 * provide friendly update like update(name = "me", age=10)
 * provide friendly finder find((name, age))
 * where clause will generate autojoin
 */
object DynamicAccessors {
  def getTypeTag[T: u.TypeTag](obj: T) = u.typeTag[T]
  def updateImpl[T: c.WeakTypeTag](c: Context)(name: c.Expr[String])(args: c.Expr[(String, Any)]*): c.Expr[Int] = {
    import c.universe._
    import Flag._
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val Select(Apply(implct, query :: Nil), methodName) = c.typecheck(c.prefix.tree)
    val paramnames = args.map(_.tree).map {
      case Apply(_, List(Literal(Constant(paramname: String)), _)) => Select(Ident(TermName("row")), TermName(paramname))
    } toList
    // do we handle parts correctly here ? I doubt it.
    val tupleNames = Apply(Select(Ident(TermName("scala")), TermName("Tuple" + paramnames.length)), paramnames)

    val paramvals = args.map(_.tree).map {
      case Apply(_, List(_, paramval)) => paramval
    } toList
    val tupeVals = Apply(Select(Ident(TermName("scala")), TermName("Tuple" + paramvals.length)), paramvals)

    val update =
      if (paramnames.length == 1)
        q"$query.map(row => ${paramnames.head}).update(${paramvals.head})"
      else
        q"$query.map(row => $tupleNames).update($tupeVals)"
    c.Expr[Int](update)
  }
  def insertImpl[T: c.WeakTypeTag](c: Context)(obj: c.Expr[T]): c.Expr[Int] = {
    import c.universe._
    def getTypeTag[T: u.TypeTag](obj: T) = u.typeTag[T]
    val prefix = c.typecheck(c.prefix.tree)
    val args = c.typecheck(obj.tree)
    val instanceT = implicitly[c.WeakTypeTag[T]].tpe
    val field = prefix.tpe.members filter (member => member.name.toString == "myType") head
    val traitType = field.typeSignatureIn(prefix.tpe)
    if (traitType.typeSymbol == args.tpe.typeSymbol) {
      val result = Apply(Select(prefix, TermName("doInsert")), List(args))
      c.Expr[Int](result)
    } else
      c.abort(c.enclosingPosition, s"${args.tpe} does not conform to $traitType")
  }
  //  def doInsert(r: DefMacroData) = DefMacroTable.forInsert returning DefMacroTable.id insert r
}

object Implicits {
  implicit def productQueryToDynamicUpdateInvoker[T, C[_]](q: LQuery[_, T, C]) = new {
    def doUpdate = new Dynamic {
      def applyDynamicNamed(name: String)(args: (String, Any)*): Int = macro DynamicAccessors.updateImpl[T]
    }
  }
}

trait DynamicAccessors[T] {
  implicit val myType: T = implicitly
  def doInsert[T <: AnyRef](obj: T): Int = macro DynamicAccessors.insertImpl[T]
  /*
   * 	def doWhere = macro whereImpl
	    def doDelete = macro deleteImpl
	    def doUpdate = macro deleteImpl
	    def doFind = macro findImpl
	*/
}
