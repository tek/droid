package slick.test

import scala.slick.driver.SQLiteDriver.simple._

import slick._
import slick.db._

import org.specs2._
import org.specs2.specification._

abstract class SlickTest
extends Specification
with BeforeAll
{
  implicit val dbInfo = slick.db.DBConnectionInfo(
    url = s"jdbc:sqlite:slick/target/slick_test.db",
    driverClassName = "org.sqlite.JDBC"
  )

  def db = Database.forURL(dbInfo.url, null, null, null,
    dbInfo.driverClassName)
}
