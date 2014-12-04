package tryp.droid.util

import scala.collection.mutable.Buffer

object SeqExt
{
  implicit class SeqExt[A](seq: Buffer[A])
  {
    def insertBy[B](item: A)(predicate: (B, B) => Boolean)(getter: (A) => B) {
      seq.indexWhere(it => predicate(getter(it), getter(item))) match {
        case -1 => seq += item
        case index => seq.insert(index, item)
      }
    }
  }
}
