package tryp
package droid

import scalaz._, Scalaz._

import argonaut._, Argonaut._

import ViewState._
import UiActionTypes._

abstract class ShowStateImpl[A <: Model: DecodeJson]
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: AndroidUiContext,
  broadcast: Broadcaster)
extends StateImpl
{
  case class Model(model: A)
  extends Data

  case class SetModel(model: A)
  extends Message

  def name: String

  override def description = s"state for show $name"

  def setupData(args: Map[String, String]) = {
    def errmsg(item: String) = {
      s"No valid $item passed to show impl for '$name'"
    }
    args.get(Keys.model)
      .flatMap(_.decodeOption[A])
      .toDbioSuccess
      .nelM(errmsg("model"))
      .orElse {
        args.get(Keys.dataId)
          .flatMap(id ⇒ Try(ObjectId(id)).toOption)
          .toDbioSuccess
          .nelM(errmsg("dataId"))
          .nelFlatMap { a ⇒
            fetchData(a) nelM(s"fetchData failed for $a")
          }
      }
      .vmap(SetModel.apply)
  }

  def fetchData(id: ObjectId): AnyAction[Option[A]]

  def fetchDetails(m: A): AppEffect = Nop

  def updateData(m: A): AppEffect

  def create(args: Map[String, String], state: Option[Bundle])
  : ViewTransition = {
    case S(Pristine, data) ⇒
      S(Initializing, data) << setupData(args)
  }

  val resume: ViewTransition = {
    case s @ S(_, _) ⇒
      s
  }

  def model(m: A): ViewTransition = {
    case S(Initializing, data) ⇒
      S(Initialized, Model(m)) << Update
  }

  def update: ViewTransition = {
    case s @ S(Initialized, Model(m)) ⇒
      s << updateData(m) << fetchDetails(m)
  }

  val transitions: ViewTransitions = {
    case Create(args, state) ⇒ create(args, state)
    case Resume ⇒ resume
    case Update ⇒ update
    case SetModel(m) ⇒ model(m)
  }
}
