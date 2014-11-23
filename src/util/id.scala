package tryp.droid.util

import scala.language.dynamics

import android.view.View

import macroid.FullDsl.{id => twId,_}
import macroid.Tweak

case class Id(value: Int)

class IdGen(start: Int) extends Dynamic {
  var ids = Map[String, Id]()
  private var counter = start

  private val lock = new Object

  def selectDynamic(tag: String): Id = lock synchronized {
    ids.getOrElse(tag, {
      counter += 1
      ids += tag → counter
      counter
    })
  }
}

object Id extends IdGen(1000)
{
  def apply(name: String) = {
    selectDynamic(name)
  }

  implicit def `Id from Int`(value: Int): Id = new Id(value)
  implicit def `Int from Id`(id: Id) = id.value
  implicit def `Tweak from Id`(id: Id): Tweak[View] = twId(id.value)
}

object Tag extends Dynamic
{
  def selectDynamic(tag: String) = tag

  def apply(name: String) = {
    selectDynamic(name)
  }
}
