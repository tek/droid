package tryp.droid

import scalaz._, Scalaz._

import macroid.{FragmentManagerContext, ContextWrapper, CanTweak}
import macroid.FullDsl._

trait MacroidExt
{
  implicit class FragmentBuilderExt[A](fb: macroid.FragmentBuilder[A])(
    implicit ct: ClassTag[A]
  )
  {
    def f[M](implicit managerCtx: FragmentManagerContext[A, M]) = {
      val name = ct.runtimeClass.getSimpleName
      fb.framed(Id(name), Tag(name))
    }
  }

  implicit def `Extract Ui from Option`[A](o: Option[Ui[A]]) =
    o getOrElse Ui.nop

  implicit def `Widget is tweakable with Option`[W <: View, T <: Tweak[W]] =
    new CanTweak[W, Option[T], W] {
      def tweak(w: W, o: Option[T]) = Ui {
        o foreach { _(w) }
        w
      }
    }

  implicit def `Widget is tweakable with Maybe`[W <: View, T <: Tweak[W]] =
    new CanTweak[W, Maybe[T], W] {
      def tweak(w: W, m: Maybe[T]) = Ui {
        m map(_(w))
        w
      }
    }

  implicit class UiOps[A](u: Ui[A]) {
    def thenDb(action: AnyAction[_])(implicit dbi: tryp.slick.DbInfo, ec: EC) =
    {
      u map(_ ⇒ action.! recover { case e ⇒ Log.e(e); e })
    }
  }
}
