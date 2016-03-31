package tryp
package droid
package state

import droid.core._

trait DroidDBMachine
extends Machine
{
  implicit def db: tryp.slick.DbInfo
}
