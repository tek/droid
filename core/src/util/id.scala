package tryp.util

import scala.language.dynamics
import scala.collection.mutable.{Map ⇒ MMap}

import android.view.View

import macroid.FullDsl.{id ⇒ twId,_}
import macroid.{Tweak,CanTweak,Ui}

case class Id(value: Int, tag: String)

class IdGen(start: Int) extends Dynamic {
  val ids = MMap[String, Id]()

  var counter = start

  private val lock = new Object

  def selectDynamic(tag: String): Id = create(tag)

  def create(tag: String): Id = lock synchronized {
    ids.getOrElse(tag, {
      counter += 1
      val id = new Id(counter, tag)
      ids += tag → id
      id
    })
  }

  def next = create((counter + 1).toString)
}

object Id extends IdGen(1000)
{
  def apply(tag: String) = create(tag)

  implicit def `Id from Int`(value: Int): Id = new Id(value, "from Int")
  implicit def `Int from Id`(id: Id) = id.value

  implicit def `Widget is tweakable with Id`[W <: View] =
    new CanTweak[W, Id, W] {
      def tweak(w: W, i: Id) = Ui { twId(i.value)(w); w }
    }
}

object Tag extends Dynamic
{
  def selectDynamic(tag: String) = tag

  def apply(name: String) = {
    selectDynamic(name)
  }
}
