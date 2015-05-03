package slick.test

import scala.slick.driver.SQLiteDriver.simple._

import slick.{schema, Schema}
import slick.db.DBSession

import org.specs2._
import org.specs2.specification._


@Schema()
object SimpleTestSchema
{
  case class Alpha(name: String)
  case class Beta(name: String, alp: Alpha)
  case class Gamma(name: String, bet: List[Beta])
}

@Schema()
object ExtTestSchema
extends schema.Timestamps
with schema.Uuids
with schema.Sync
{
  case class Alpha(name: String)
  case class Beta(name: String, alp: Alpha, alp2: Alpha)
  case class Gamma(name: String, bet: List[Beta], bet2: List[Beta])
}

class SimpleSchemaTest
extends Specification
{
  def is = s2"""
  The simple schema should

  work $e1
  """

  def e1 = {
    1 must_== 1
  }
}

class ExtSchemaTest
extends Specification
with BeforeAll
{
  def is = s2"""
  The extended schema should

  create models $checkModels
  add list associations $listAssoc
  add flat associations $flatAssoc
  create pending actions $pendingAct
  """

  import ExtTestSchema._

  implicit val dbInfo = slick.db.DBConnectionInfo(
    url = s"jdbc:sqlite:slick/target/slick_test.db",
    driverClassName = "org.sqlite.JDBC"
  )

  def db = Database.forURL(dbInfo.url, null, null, null,
    dbInfo.driverClassName)

  @DBSession def beforeAll() {
    metadata.dropAll()
    pendingMetadata.dropAll()
    metadata.createMissingTables()
    pendingMetadata.createMissingTables()
  }

  def createModels = {
    db withSession { implicit s ⇒
      for {
        a ← alphas.insert(Alpha(None, "something"))
        aId ← a.id
        a2 ← alphas.insert(Alpha(None, "something else"))
        a2Id ← a2.id
        b ← betas.insert(Beta(None, "yello", aId, a2Id))
        bId ← b.id
        c ← gammas.insert(Gamma(None, "hello"))

      } yield (a, b, bId, c)
    }
  }

  lazy val models = createModels.get
  
  def checkModels() = Try(models) must beSuccessfulTry

  def listAssoc = {
    db withSession { implicit s ⇒
    val (a, b, bId, c) = models
      c.addBet(bId)
      c.addBet2(bId)
      c.loadBets.list must contain(b)
    }
  }

  def flatAssoc = {
    db withSession { implicit s ⇒
      val (a, b, bId, c) = models
      b.loadAlp must_!= b.loadAlp2
    }
  }

  def pendingAct = {
    import ExtTestSchema.PendingActionsSchema._
    db withSession { implicit s ⇒
      val (a, b, bId, c) = models
      val aAdds = for {
        a ← pendingActions if a.model === "alphas"
        add ← a.additions
      } yield add
      val bAdds = for {
        ba ← pendingActions if ba.model === "betas"
        bad ← ba.additions
      } yield bad
      val cAdds = for {
        ca ← pendingActions if ca.model === "gammas"
        cad ← ca.additions
      } yield cad
      aAdds.list === List(Addition(Some(1),1), Addition(Some(2),2)) &&
        bAdds.list === List(Addition(Some(3),1)) &&
        cAdds.list === List(Addition(Some(4),1))
    }
  }
}
