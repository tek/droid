package tryp
package droid
package core

import reflect.macros.blackbox

import android.content.res.{TypedArray, ColorStateList}
import android.graphics.drawable.Drawable

import cats._
import cats.data.{NonEmptyList, OneAnd, Validated, ValidatedNel}
import cats.instances.list._
import cats.syntax.traverse._

class Theme(implicit internal: ThemeInternal)
{
  def drawable(name: String): Throwable Either Drawable = {
    styledAttribute(name, _.getDrawable(0))
  }

  def color(name: String): Throwable Either Int = {
    styledAttribute(name, _.getColor(0, 0))
  }

  def dimension(name: String, defValue: Float = 10) = {
    styledAttribute(name, _.getDimension(0, defValue))
  }

  def colorStateList(name: String): Throwable Either ColorStateList = {
    styledAttribute(name, _.getColorStateList(0))
  }

  def styledAttribute[T](name: String, getter: TypedArray => T) = {
    styledAttributes(List(name), getter)
  }

  def styledAttributes[T]
  (names: List[String], getter: TypedArray => T): Throwable Either T = {
    internal.styledAttrs(names) flatMap { attrs =>
      Either.catchNonFatal(getter(attrs)) tap(_ => attrs.recycle)
    }
  }
}

object Theme
{
  implicit def fromContext(implicit con: Context): Theme = new Theme
}

trait ThemeInternal
{
  def styledAttrs(names: List[String]): Throwable Either TypedArray
}

object ThemeInternal
{
  // implicit def materialize(implicit con: Context): ThemeInternal =
  //   macro ThemeInternalMacros.materializeFromContext

  implicit def fromContext(implicit con: Context): ThemeInternal = {
    new AndroidThemeInternal
  }
}

class ThemeInternalMacros(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._

  def materializeFromContext(con: c.Expr[Context]) =
    q"new AndroidThemeInternal($con)"
}

class AndroidThemeInternal(implicit context: Context, res: ResourcesInternal)
extends ThemeInternal
{
  def styledAttrs(names: List[String]): Throwable Either TypedArray = {
    cats.Traverse.ops.toAllTraverseOps(names).traverseU(attrId)
    .leftMap(a => new Throwable(a))
    .flatMap { attrIds =>
      Either.catchNonFatal {
        context.obtainStyledAttributes(attrIds.toArray)
      }
    }
  }

  def attrId(name: String) = res.identifier("attr", name)
}
