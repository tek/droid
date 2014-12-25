package tryp.droid.util

import scala.reflect.ClassTag

import android.app.Activity

import macroid.{FragmentManagerContext,ActivityContext,AppContext}

import tryp.droid.view.ActivityContexts

object MacroidExt
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
}
