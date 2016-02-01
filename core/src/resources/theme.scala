package tryp
package droid
package core

import reflect.macros.blackbox

import android.content.Context
import android.content.res.{TypedArray,ColorStateList}
import android.graphics.drawable.Drawable

import cats._
import cats.data.{NonEmptyList, OneAnd, Validated, ValidatedNel, Xor}
import cats.std.list._
import cats.syntax.traverse._

class Theme(implicit internal: ThemeInternal)
{
  def drawable(name: String): Throwable Xor Drawable = {
    styledAttribute(name, _.getDrawable(0))
  }

  def color(name: String): Throwable Xor Int = {
    styledAttribute(name, _.getColor(0, 0))
  }

  def dimension(name: String, defValue: Float = 10) = {
    styledAttribute(name, _.getDimension(0, defValue))
  }

  def colorStateList(name: String): Throwable Xor ColorStateList = {
    styledAttribute(name, _.getColorStateList(0))
  }

  def styledAttribute[T](name: String, getter: TypedArray ⇒ T) = {
    styledAttributes(List(name), getter)
  }

  def styledAttributes[T]
  (names: List[String], getter: TypedArray ⇒ T): Throwable Xor T = {
    internal.styledAttrs(names) flatMap { attrs ⇒
      Xor.catchNonFatal(getter(attrs)) tap(_ ⇒ attrs.recycle)
    }
  }
}

object Theme
{
  implicit def fromContext(implicit con: Context): Theme = new Theme
}

trait ThemeInternal
{
  def styledAttrs(names: List[String]): Throwable Xor TypedArray
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
  def styledAttrs(names: List[String]): Throwable Xor TypedArray = {
    names.traverseU(attrId)
    .leftMap(a ⇒ new Throwable(a))
    .flatMap { attrIds ⇒
      Xor.catchNonFatal {
        context.obtainStyledAttributes(attrIds.toArray)
      }
    }
  }

  def attrId(name: String) = res.identifier("attr", name)
}
