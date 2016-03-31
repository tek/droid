package tryp
package droid
package core

import android.view._

import scala.language.dynamics

case class RId(value: Int, tag: String)

class RIdGen(start: Int) extends Dynamic {
  val ids = MMap[String, RId]()

  var counter = start

  private val lock = new Object

  def selectDynamic(tag: String): RId = create(tag)

  def create(tag: String): RId = lock synchronized {
    ids.getOrElse(tag, {
      counter += 1
      val id = new RId(counter, tag)
      ids += tag -> id
      id
    })
  }

  def next = create((counter + 1).toString)
}

object RId extends RIdGen(1000)
{
  def apply(tag: String) = create(tag)

  implicit def `RId from Int`(value: Int): RId = new RId(value, "from Int")
  implicit def `Int from RId`(id: RId) = id.value
}

object Tag extends Dynamic
{
  def selectDynamic(tag: String) = tag

  def apply(name: String) = {
    selectDynamic(name)
  }
}
