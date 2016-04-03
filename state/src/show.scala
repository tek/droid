package tryp
package droid
package state

import scalaz._, Scalaz._

import shapeless.tag.@@

import argonaut._, Argonaut._

import droid.core.Keys
import droid.state.core._

abstract class ShowMachine[A <: Model: DecodeJson]
extends Machine
{
  case class Model(model: A)
  extends Data

  case class SetModel(model: A)
  extends Message

  override def description = s"state for show $handle"

  override def machinePrefix: List[String] = "show" :: super.machinePrefix

  def setupData(args: Map[String, String]): droid.core.IOActionTypes.Action[SetModel] = {
    def errmsg(item: String) = {
      s"No valid $item passed to show impl for '$handle'"
    }
    args.get(Keys.model)
      .flatMap(_.decodeOption[A])
      .toDbioSuccess
      .nelM(errmsg("model"))
      .orElse {
        args.get(Keys.dataId)
          .flatMap(id => Try(ObjectId(id)).toOption)
          .toDbioSuccess
          .nelM(errmsg("dataId"))
          .nelFlatMap { a =>
            fetchData(a) nelM(s"fetchData failed for $a")
          }
      }
      .vmap(SetModel.apply)
  }

  def fetchData(id: ObjectId): AnyAction[Option[A]]

  def fetchDetails(m: A): Effect = Nop

  def updateData(m: A): Effect

  def create(args: Map[String, String], state: Option[Bundle]): Transit = {
    case S(Pristine, data) =>
      S(Initializing, data) << setupData(args)
  }

  val resume: Transit = {
    case s @ S(_, _) =>
      s
  }

  def model(m: A): Transit = {
    case S(Initializing, data) =>
      S(Initialized, Model(m)) << Update
  }

  def update: Transit = {
    case s @ S(Initialized, Model(m)) =>
      s << updateData(m) << fetchDetails(m)
  }

  val admit: Admission = {
    case Create(args, state) => create(args, state)
    case Resume => resume
    case Update => update
    case SetModel(m) => model(m)
  }
}