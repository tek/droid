package tryp.droid

import macroid.{FragmentManagerContext,ActivityContext,AppContext,CanTweak}
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
}
