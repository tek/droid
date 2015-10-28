package tryp.droid

import scala.language.implicitConversions

object Sender {
  case class Send[T >: Null <: AnyRef](cls: T) {
    def send(methodName: String, args: AnyRef*): AnyRef = {
      val argtypes = args.map(_.getClass)
      val method = cls.getClass.getMethod(methodName, argtypes: _*)
      method.invoke(cls, args: _*)
    }

    def sendParams(methodName: String, params: Params): AnyRef = {
      val method = cls.getClass.getMethod(methodName, classOf[Params])
      method.invoke(cls, params)
    }
  }

  implicit def sendable[T >: Null <: AnyRef](cls: T): Send[T] = {
    new Send(cls)
  }
}
