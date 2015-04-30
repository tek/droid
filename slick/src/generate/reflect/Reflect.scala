package slickmacros.reflect

import scala.reflect.runtime.universe._
import scala.slick.profile._

case class TypeDesc(name: String, params: List[String])
case class ParamDesc(name: String, tpe: TypeDesc)
case class MemberDesc(name: String, tpe: TypeDesc, params: List[ParamDesc])
case class ClassDesc(name: String, isTable: Boolean, members: List[MemberDesc])

class ObjectRef(any: AnyRef) {
  val typeMirror = runtimeMirror(any.getClass.getClassLoader)
  val instanceMirror = typeMirror.reflect(any)
  val members = instanceMirror.symbol.typeSignature.members
  def typeDesc(t: Type) = {
    try {
      val tpe = t.asInstanceOf[TypeRefApi]
      TypeDesc(tpe.sym.name.toString, tpe.args.map(_.typeSymbol.name.toString))
    } catch {
      case e: Throwable =>
        val tpe = t.asInstanceOf[TypeApi]
        TypeDesc(tpe.typeSymbol.name.toString, List())
    }
  }
  def tables = members.filter(_.typeSignature <:< typeOf[RelationalTableComponent#Table[_]])
  
  def reflect: List[ClassDesc] = {
    def fieldMirror(symbol: Symbol) = instanceMirror.reflectField(symbol.asTerm)

    members.collect {
      case s if s.isClass && (s.asClass.isCaseClass || s.asClass.baseClasses.exists(_.name.toString == "Table")) =>
        val c = s.asClass
        val isTable = c.baseClasses.exists(_.name.toString == "Table")
        val t = c.toType
        val membersDesc = t.members.flatMap { m =>
          if (!m.isMethod) {
            Some(MemberDesc(m.name.toString, typeDesc(m.typeSignature), List()))
          } else {
            val f = m.asMethod
            val t = f.typeSignature

            val ignore = List("equals", "toString", "hashCode", "canEqual", "productIterator", "productElement", "productArity", "productPrefix", "copy")
            val toIgnore = ignore.exists(f.name.toString.startsWith(_))
            if (f.owner == c && !f.isGetter && !f.isSetter && !f.isConstructor && !toIgnore) {
              val retType = typeDesc(f.returnType)
              val paramsDesc = f.paramLists.flatMap { p =>
                p.map { s => ParamDesc(s.name.toString, typeDesc(s.typeSignature))
                }
              }
              Some(MemberDesc(f.name.toString, retType, paramsDesc))
            } else {
              None
            }
          }
        } toList;
        ClassDesc(c.name.toString, isTable, membersDesc)
    } toList;
  }
}
